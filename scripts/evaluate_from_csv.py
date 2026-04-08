"""Quick local evaluator: load an exported evaluation CSV and print metrics.

Usage:
  python scripts\evaluate_from_csv.py --csv evaluation_manual_label_reanalyzed.csv

This is useful when you already have the CSV produced by
`evaluate_sentiment.py` and want to compute/see the classification report
and confusion matrix locally without connecting to the DB.
"""
import re
import argparse
import pandas as pd
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, f1_score

CANONICAL = ["LOVE", "LIKE", "NEUTRAL", "DISLIKE", "TERRIBLE"]

NUM_TO_CANON = {
    4: "LOVE",
    3: "LIKE",
    2: "NEUTRAL",
    1: "DISLIKE",
    0: "TERRIBLE",
}

LABEL_TEXT_TO_CANON = {
    "very positive": "LOVE",
    "positive": "LIKE",
    "neutral": "NEUTRAL",
    "negative": "DISLIKE",
    "very negative": "TERRIBLE",
    "love": "LOVE",
    "like": "LIKE",
    "neutral": "NEUTRAL",
    "dislike": "DISLIKE",
    "terrible": "TERRIBLE",
    # greek common examples
    "θετικό": "LIKE",
    "πολύ θετικό": "LOVE",
    "ουδέτερο": "NEUTRAL",
    "αρνητικό": "DISLIKE",
    "πολύ αρνητικό": "TERRIBLE",
}


def normalize_label(raw):
    if raw is None:
        return None
    if isinstance(raw, (int,)):
        return NUM_TO_CANON.get(raw)
    s = str(raw).strip()
    if not s:
        return None
    up = s.upper()
    if up in CANONICAL:
        return up
    low = s.lower()
    for k, v in LABEL_TEXT_TO_CANON.items():
        if low == k:
            return v
    for k, v in LABEL_TEXT_TO_CANON.items():
        if k in low:
            return v
    m = re.search(r"\b([0-4])\b", s)
    if m:
        return NUM_TO_CANON.get(int(m.group(1)))
    short = re.sub(r"\W+", "", up)
    if short in CANONICAL:
        return short
    return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--csv', type=str, default='evaluation_manual_label_reanalyzed.csv', help='Path to CSV exported by evaluate_sentiment.py')
    args = parser.parse_args()

    df = pd.read_csv(args.csv)
    # prefer normalized columns if present
    if 'gold_norm' in df.columns and 'pred_norm' in df.columns:
        y_true = df['gold_norm'].tolist()
        y_pred = df['pred_norm'].tolist()
    else:
        # try to normalize from gold_label / pred_label / pred_class
        def get_pred_row(r):
            return normalize_label(r.get('pred_label')) or normalize_label(r.get('pred_class'))

        df['gold_norm_local'] = df.get('gold_label').apply(normalize_label) if 'gold_label' in df.columns else None
        df['pred_norm_local'] = df.apply(lambda r: get_pred_row(r), axis=1)
        if 'gold_norm_local' in df.columns:
            valid = df.dropna(subset=['gold_norm_local','pred_norm_local']).copy()
            y_true = valid['gold_norm_local'].tolist()
            y_pred = valid['pred_norm_local'].tolist()
        else:
            raise SystemExit('CSV does not contain usable label columns (gold_norm or gold_label)')

    labels = CANONICAL
    acc = accuracy_score(y_true, y_pred)
    macro_f1 = f1_score(y_true, y_pred, labels=labels, average='macro')
    weighted_f1 = f1_score(y_true, y_pred, labels=labels, average='weighted')

    print('\n=== Evaluation Summary ===')
    print(f'Examples used: {len(y_true)}')
    print(f'Accuracy: {acc:.4f}')
    print(f'Macro F1: {macro_f1:.4f}')
    print(f'Weighted F1: {weighted_f1:.4f}\n')

    print('Classification report (per class):\n')
    print(classification_report(y_true, y_pred, labels=labels, digits=4))

    print('Confusion matrix (rows=gold, cols=pred):\n')
    cm = confusion_matrix(y_true, y_pred, labels=labels)
    print(pd.DataFrame(cm, index=labels, columns=labels))


if __name__ == '__main__':
    main()


