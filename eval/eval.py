#!/usr/bin/env python3
"""
Tổng hợp chỉ số đánh giá VIAccess (PLAN mục 6). Chỉ dùng thư viện chuẩn Python 3.8+.

Cách dùng (chạy trong thư mục eval/):
  python eval.py sus        data/sus.csv
  python eval.py tlx        data/tlx.csv
  python eval.py ab         data/tasks.csv  --metric time_s
  python eval.py detection  data/detection.csv
  python eval.py ocr        data/ocr.csv
  python eval.py latency    data/metrics.csv

Định dạng file: xem eval/README.md và các file mẫu trong eval/templates/.
"""
import argparse
import csv
import math
import statistics
import sys
from collections import defaultdict


# ---------------------------------------------------------------- tiện ích
def read_rows(path):
    with open(path, newline="", encoding="utf-8-sig") as f:
        return list(csv.DictReader(f))


def describe(values):
    if not values:
        return "n=0"
    s = f"n={len(values)} mean={statistics.mean(values):.2f}"
    if len(values) > 1:
        s += f" sd={statistics.stdev(values):.2f}"
    s += f" median={statistics.median(values):.2f} min={min(values):.2f} max={max(values):.2f}"
    return s


def wilcoxon_signed_rank(a, b):
    """Kiểm định Wilcoxon cặp đôi, hai phía. Trả về (W, p, n_hiệu_khác_0).
    n ≤ 20: p chính xác bằng liệt kê phân phối; n > 20: xấp xỉ chuẩn có hiệu chỉnh hạng trùng."""
    diffs = [y - x for x, y in zip(a, b) if y - x != 0]
    n = len(diffs)
    if n == 0:
        return 0.0, 1.0, 0
    order = sorted(range(n), key=lambda i: abs(diffs[i]))
    ranks = [0.0] * n
    i = 0
    tie_term = 0.0
    while i < n:
        j = i
        while j + 1 < n and abs(diffs[order[j + 1]]) == abs(diffs[order[i]]):
            j += 1
        r = (i + j) / 2 + 1
        t = j - i + 1
        tie_term += t ** 3 - t
        for k in range(i, j + 1):
            ranks[order[k]] = r
        i = j + 1
    w_plus = sum(r for r, d in zip(ranks, diffs) if d > 0)
    w_minus = sum(r for r, d in zip(ranks, diffs) if d < 0)
    w = min(w_plus, w_minus)

    if n <= 20 and tie_term == 0:
        # Phân phối chính xác của W+ dưới H0.
        max_sum = n * (n + 1) // 2
        counts = [0] * (max_sum + 1)
        counts[0] = 1
        for k in range(1, n + 1):
            for s in range(max_sum, k - 1, -1):
                counts[s] += counts[s - k]
        total = 2 ** n
        p = 2 * sum(counts[: int(w) + 1]) / total
        return w, min(1.0, p), n

    mean = n * (n + 1) / 4
    var = n * (n + 1) * (2 * n + 1) / 24 - tie_term / 48
    z = (w - mean + 0.5) / math.sqrt(var) if var > 0 else 0.0
    p = 2 * 0.5 * math.erfc(abs(z) / math.sqrt(2))
    return w, min(1.0, p), n


def levenshtein(a, b):
    prev = list(range(len(b) + 1))
    for i, ca in enumerate(a, 1):
        cur = [i]
        for j, cb in enumerate(b, 1):
            cur.append(min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (ca != cb)))
        prev = cur
    return prev[-1]


# ---------------------------------------------------------------- lệnh
def cmd_sus(args):
    """SUS: cột participant, condition (A/B), q1..q10 (1–5). Điểm = 2.5 × tổng đóng góp."""
    by_cond = defaultdict(dict)
    for row in read_rows(args.file):
        contrib = 0
        for k in range(1, 11):
            v = int(row[f"q{k}"])
            contrib += (v - 1) if k % 2 == 1 else (5 - v)
        by_cond[row["condition"]][row["participant"]] = contrib * 2.5
    for cond, scores in sorted(by_cond.items()):
        print(f"SUS {cond}: {describe(list(scores.values()))}")
    paired(by_cond, "SUS")


def cmd_tlx(args):
    """NASA-TLX raw: cột participant, condition, mental, physical, temporal, performance, effort, frustration (0–100)."""
    dims = ["mental", "physical", "temporal", "performance", "effort", "frustration"]
    by_cond = defaultdict(dict)
    for row in read_rows(args.file):
        by_cond[row["condition"]][row["participant"]] = statistics.mean(float(row[d]) for d in dims)
    for cond, scores in sorted(by_cond.items()):
        print(f"NASA-TLX {cond}: {describe(list(scores.values()))}")
    paired(by_cond, "NASA-TLX")


def cmd_ab(args):
    """Nhiệm vụ: cột participant, condition, task, completed (0/1), time_s, errors.
    Lấy trung bình theo người cho mỗi điều kiện rồi so sánh cặp A/B."""
    per = defaultdict(lambda: defaultdict(list))
    done = defaultdict(lambda: [0, 0])
    for row in read_rows(args.file):
        per[row["condition"]][row["participant"]].append(float(row[args.metric]))
        done[row["condition"]][0] += int(row.get("completed", 1) or 0)
        done[row["condition"]][1] += 1
    by_cond = {c: {p: statistics.mean(v) for p, v in d.items()} for c, d in per.items()}
    for cond in sorted(by_cond):
        ok, total = done[cond]
        print(f"{args.metric} {cond}: {describe(list(by_cond[cond].values()))}  hoàn thành {ok}/{total}")
    paired(by_cond, args.metric)


def paired(by_cond, label):
    if not {"A", "B"} <= set(by_cond):
        return
    common = sorted(set(by_cond["A"]) & set(by_cond["B"]))
    if len(common) < 2:
        print(f"Wilcoxon {label}: cần ≥ 2 người có cả A và B")
        return
    a = [by_cond["A"][p] for p in common]
    b = [by_cond["B"][p] for p in common]
    w, p, n = wilcoxon_signed_rank(a, b)
    print(f"Wilcoxon {label} (A vs B, {len(common)} cặp, {n} khác 0): W={w:.1f} p={p:.4f}")


def cmd_detection(args):
    """Nhận diện: mỗi dòng 1 ảnh: image, light (sang/toi), gt (lớp đúng hoặc 'none'), pred (lớp app báo hoặc 'none')."""
    rows = read_rows(args.file)
    for light in sorted({r.get("light", "") for r in rows}) + [None]:
        subset = [r for r in rows if light is None or r.get("light", "") == light]
        classes = sorted({r["gt"] for r in subset if r["gt"] != "none"} | {r["pred"] for r in subset if r["pred"] != "none"})
        title = "TẤT CẢ" if light is None else f"ánh sáng={light}"
        print(f"\n== {title} ({len(subset)} ảnh)")
        f1s = []
        for c in classes:
            tp = sum(1 for r in subset if r["gt"] == c and r["pred"] == c)
            fp = sum(1 for r in subset if r["gt"] != c and r["pred"] == c)
            fn = sum(1 for r in subset if r["gt"] == c and r["pred"] != c)
            p = tp / (tp + fp) if tp + fp else 0.0
            rc = tp / (tp + fn) if tp + fn else 0.0
            f1 = 2 * p * rc / (p + rc) if p + rc else 0.0
            f1s.append((p, rc, f1))
            print(f"{c:15s} P={p:.2f} R={rc:.2f} F1={f1:.2f} (TP={tp} FP={fp} FN={fn})")
        if f1s:
            print(f"{'macro':15s} P={statistics.mean(x[0] for x in f1s):.2f} "
                  f"R={statistics.mean(x[1] for x in f1s):.2f} F1={statistics.mean(x[2] for x in f1s):.2f}")


def cmd_ocr(args):
    """OCR: cột sample, light, reference, hypothesis, auto_captured (0/1, tuỳ chọn)."""
    rows = read_rows(args.file)
    groups = defaultdict(list)
    for r in rows:
        ref, hyp = r["reference"].strip(), r["hypothesis"].strip()
        cer = levenshtein(ref, hyp) / max(1, len(ref))
        wer = levenshtein(ref.split(), hyp.split()) / max(1, len(ref.split()))
        groups[r.get("light", "")].append((cer, wer, r.get("auto_captured", "")))
    for light, vals in sorted(groups.items()):
        cers = [v[0] for v in vals]
        wers = [v[1] for v in vals]
        caps = [int(v[2]) for v in vals if v[2] != ""]
        line = f"ánh sáng={light or '-'}: CER {describe(cers)} | WER mean={statistics.mean(wers):.3f}"
        if caps:
            line += f" | tự chụp thành công {sum(caps)}/{len(caps)} = {sum(caps) / len(caps):.0%}"
        print(line)


def cmd_latency(args):
    """Log CSV xuất từ app (MetricLogger): timestamp_utc, elapsed_realtime_ns, frame_id, event, ..., latency_ms.
    In thời gian suy luận và thời gian frame → phản hồi (haptic_start / tts_start đầu tiên sau frame)."""
    rows = read_rows(args.file)
    infer = [float(r["latency_ms"]) for r in rows if r["event"] == "inference_complete" and r["latency_ms"]]
    print(f"Suy luận YOLO (ms): {describe(infer)}")
    frames = {}
    for r in rows:
        if r["event"] in ("find_found", "baseline_detection") and r["frame_id"]:
            frames.setdefault(r["frame_id"], None)
    starts = {r["frame_id"]: int(r["elapsed_realtime_ns"]) for r in rows if r["event"] == "frame_start" and r["frame_id"]}
    outputs = sorted(int(r["elapsed_realtime_ns"]) for r in rows if r["event"] in ("haptic_start", "tts_start"))
    resp = []
    for fid in frames:
        t0 = starts.get(fid)
        if t0 is None:
            continue
        nxt = next((t for t in outputs if t >= t0), None)
        if nxt is not None:
            resp.append((nxt - t0) / 1e6)
    print(f"Frame → phản hồi (ms): {describe(resp)}")
    per_min = defaultdict(lambda: [0, 0])
    for r in rows:
        minute = r["timestamp_utc"][:16]
        if r["event"] == "tts_start":
            per_min[minute][0] += 1
        elif r["event"] == "haptic_start":
            per_min[minute][1] += 1
    if per_min:
        speech = [v[0] for v in per_min.values()]
        haptic = [v[1] for v in per_min.values()]
        print(f"Câu nói / phút: {describe(speech)}")
        print(f"Lần rung / phút: {describe(haptic)}")


def main(argv=None):
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except AttributeError:
        pass
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    for name in ("sus", "tlx", "detection", "ocr", "latency"):
        sub.add_parser(name).add_argument("file")
    ab = sub.add_parser("ab")
    ab.add_argument("file")
    ab.add_argument("--metric", default="time_s")
    args = ap.parse_args(argv)
    {"sus": cmd_sus, "tlx": cmd_tlx, "ab": cmd_ab, "detection": cmd_detection,
     "ocr": cmd_ocr, "latency": cmd_latency}[args.cmd](args)


if __name__ == "__main__":
    sys.exit(main())
