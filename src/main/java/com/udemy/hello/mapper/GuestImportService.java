package com.udemy.hello.mapper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udemy.hello.model.Note;
import com.udemy.hello.model.Plan;

/**
 * ゲストモードで作成したプラン・メモを、ログイン後のアカウントへ取り込む。
 * 販売可否評価レポート「4.5 ゲストデータがログイン後に引き継がれない」への対応。
 *
 * ゲストデータのplans[].id / parent_id、notes[].linksは、いずれも
 * localStorage側で振られたローカルID（DBには存在しない）。新規プランを
 * 挿入するたびにローカルID→実IDの対応表を作り、それを使って親子関係と
 * メモのリンク先を実IDへ張り直す。
 */
@Service
public class GuestImportService {

	// 単発の取り込みで許容する上限。ゲスト側の上限（プラン25件・メモ30件）より
	// 十分大きくしてあるが、無制限に受け付けると悪意あるリクエストで
	// 大量挿入されうるため、念のため上限を設ける
	private static final int MAX_PLANS_PER_IMPORT = 200;
	private static final int MAX_NOTES_PER_IMPORT = 200;
	// note_insertエンドポイントと同じ、フリープランの登録上限
	private static final int FREE_NOTE_LIMIT = 100;

	@Autowired
	private PlanService planService;

	@Autowired
	private NoteService noteService;

	public static class Result {
		public final int importedPlans;
		public final int importedNotes;
		public final int skippedNotes; // フリープラン上限に達し取り込めなかった件数

		public Result(int importedPlans, int importedNotes, int skippedNotes) {
			this.importedPlans = importedPlans;
			this.importedNotes = importedNotes;
			this.skippedNotes = skippedNotes;
		}
	}

	@Transactional
	public Result importGuestData(int userId, List<Plan> guestPlans, List<Note> guestNotes) {
		if (guestPlans == null) guestPlans = List.of();
		if (guestNotes == null) guestNotes = List.of();
		if (guestPlans.size() > MAX_PLANS_PER_IMPORT || guestNotes.size() > MAX_NOTES_PER_IMPORT) {
			throw new IllegalArgumentException("一度に取り込める件数を超えています。");
		}

		Map<Integer, Integer> planIdMap = importPlans(userId, guestPlans);
		int importedNotes = 0;
		int skippedNotes = 0;

		int room = Math.max(0, FREE_NOTE_LIMIT - noteService.count(userId));
		for (Note guestNote : guestNotes) {
			if (importedNotes >= room) {
				skippedNotes++;
				continue;
			}
			insertImportedNote(userId, guestNote, planIdMap);
			importedNotes++;
		}

		return new Result(planIdMap.size(), importedNotes, skippedNotes);
	}

	// 親が既に取り込み済み（またはルート）のプランから順に挿入していき、
	// ローカルID→実IDの対応表を作る。循環参照など最後まで親を解決できなかった分は、
	// データを失わないよう孤立させずルート直下として取り込む
	private Map<Integer, Integer> importPlans(int userId, List<Plan> guestPlans) {
		Map<Integer, Integer> planIdMap = new LinkedHashMap<>();
		List<Plan> remaining = new ArrayList<>(guestPlans);

		while (!remaining.isEmpty()) {
			boolean progressed = false;
			Iterator<Plan> it = remaining.iterator();
			while (it.hasNext()) {
				Plan guestPlan = it.next();
				Integer localParentId = guestPlan.getParent_id();
				if (localParentId == null || planIdMap.containsKey(localParentId)) {
					Integer newParentId = localParentId == null ? null : planIdMap.get(localParentId);
					planIdMap.put(guestPlan.getId(), insertOnePlan(userId, guestPlan, newParentId));
					it.remove();
					progressed = true;
				}
			}
			if (!progressed) {
				// 親を辿っても解決しない残り（循環参照・親IDの取り違え等）は、
				// このループでは無限に進まなくなるため、一律ルート直下として取り込んで打ち切る
				for (Plan guestPlan : remaining) {
					planIdMap.put(guestPlan.getId(), insertOnePlan(userId, guestPlan, null));
				}
				remaining.clear();
			}
		}
		return planIdMap;
	}

	private Integer insertOnePlan(int userId, Plan guestPlan, Integer newParentId) {
		Plan plan = new Plan();
		plan.setParent_id(newParentId);
		plan.setTitle(guestPlan.getTitle());
		plan.setDescription(guestPlan.getDescription());
		String status = guestPlan.getStatus();
		plan.setStatus(status == null || status.isBlank() ? "not_started" : status);
		plan.setUser_id(userId);
		plan.setCreated_at(new Timestamp(System.currentTimeMillis()));
		planService.insert(plan);
		return plan.getId();
	}

	private void insertImportedNote(int userId, Note guestNote, Map<Integer, Integer> planIdMap) {
		Note note = new Note();
		note.setType(guestNote.getType());
		note.setTitle(guestNote.getTitle());
		note.setBody(guestNote.getBody());
		note.setMastery(guestNote.getMastery());
		note.setProgress(guestNote.getProgress());
		// ゲストのcategory_idはこのアカウントのカテゴリー体系とは無関係のローカルIDなので引き継がない
		note.setCategory_id(null);
		note.setReview_interval_days(guestNote.getReview_interval_days());
		note.setUser_id(userId);
		note.setCreated_at(new Timestamp(System.currentTimeMillis()));

		List<Integer> remappedLinks = new ArrayList<>();
		if (guestNote.getLinks() != null) {
			for (Integer localPlanId : guestNote.getLinks()) {
				Integer newPlanId = planIdMap.get(localPlanId);
				// 対応するプランが無ければ（データ不整合等）、そのリンクだけ落として続行する
				if (newPlanId != null) {
					remappedLinks.add(newPlanId);
				}
			}
		}
		note.setLinks(remappedLinks);

		noteService.insert(note, guestNote.getTodo_items(), guestNote.getTags());
	}
}
