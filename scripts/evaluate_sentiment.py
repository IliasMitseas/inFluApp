r"""
Evaluate sentiment analysis predictions stored in the project database.

This script connects to the PostgreSQL DB configured in
`src/main/resources/application.properties` (it will try to read DB settings
from that file) and extracts rows where we have a model prediction and a
human / ground-truth label (checks common column names). It computes accuracy,
per-class precision/recall/f1, macro/weighted f1 and confusion matrix and
exports a CSV with the examples used.

Usage (PowerShell):
  python -m venv .venv; .\.venv\Scripts\Activate.ps1; pip install -r scripts/requirements.txt; python scripts\evaluate_sentiment.py

If you prefer, set DB_* environment variables instead of relying on
`application.properties`.
"""

import re
import os
import csv
import sys
from typing import Optional, Tuple, List

import psycopg2
import pandas as pd
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, f1_score
import argparse

# default path to application.properties inside the project
APP_PROPS_PATH = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "application.properties")

# Common candidate ground-truth columns we look for (in order)
POSTS_GOLD_CANDIDATES = [
    "post_sentiment",  # enum stored on posts (Post.postSentiment)
    "manual_label",
    "human_label",
    "gold_label",
]

SENT_ANLYS_GOLD_CANDIDATES = [
    "manual_label",
    "human_label",
    "gold_label",
]

# Predicted columns in sentiment_analysis table
PRED_LABEL_COL = "label"
PRED_CLASS_COL = "sentiment_class"
PRED_POST_FK = "post_id"

# canonical labels used in the app
CANONICAL = ["LOVE", "LIKE", "NEUTRAL", "DISLIKE", "TERRIBLE"]

# mapping for numeric classes (stanford coreNLP style): 4 -> Very Positive, ...
NUM_TO_CANON = {
    4: "LOVE",
    3: "LIKE",
    2: "NEUTRAL",
    1: "DISLIKE",
    0: "TERRIBLE",
}

LABEL_TEXT_TO_CANON = {
    # English labels
    "very positive": "LOVE",
    "positive": "LIKE",
    "neutral": "NEUTRAL",
    "negative": "DISLIKE",
    "very negative": "TERRIBLE",
    # enum names
    "love": "LOVE",
    "like": "LIKE",
    "neutral": "NEUTRAL",
    "dislike": "DISLIKE",
    "terrible": "TERRIBLE",
    # greek examples (common words)
    "θετικό": "LIKE",
    "πολύ θετικό": "LOVE",
    "πολύ θετικό/εξαιρετικό": "LOVE",
    "ουδέτερο": "NEUTRAL",
    "αρνητικό": "DISLIKE",
    "πολύ αρνητικό": "TERRIBLE",
}


def parse_application_properties(path: str) -> Tuple[Optional[str], Optional[str], Optional[str], Optional[str]]:
    if not os.path.exists(path):
        return None, None, None, None
    url = username = password = None
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"): continue
            if line.startswith("spring.datasource.url="):
                url = line.split("=", 1)[1].strip()
            elif line.startswith("spring.datasource.username="):
                username = line.split("=", 1)[1].strip()
            elif line.startswith("spring.datasource.password="):
                password = line.split("=", 1)[1].strip()
    return url, username, password, path


def parse_jdbc_url(jdbc_url: str) -> Tuple[str, int, str]:
    # expect jdbc:postgresql://host:port/dbname
    m = re.match(r"jdbc:postgresql://([^:/]+)(?::(\d+))?/([a-zA-Z0-9_]+)", jdbc_url)
    if not m:
        raise ValueError(f"Cannot parse JDBC URL: {jdbc_url}")
    host = m.group(1)
    port = int(m.group(2) or 5432)
    db = m.group(3)
    return host, port, db


def column_exists(conn, table: str, column: str) -> bool:
    with conn.cursor() as cur:
        cur.execute("""
            SELECT 1 FROM information_schema.columns
            WHERE table_name = %s AND column_name = %s
            LIMIT 1
        """, (table, column))
        return cur.fetchone() is not None


def detect_ground_truth_columns(conn) -> Tuple[Optional[str], Optional[str], str]:
    # returns (table_for_gold, column_for_gold, table_for_preds)
    # 1) check posts table for candidates
    for c in POSTS_GOLD_CANDIDATES:
        if column_exists(conn, 'posts', c):
            return 'posts', c, 'sentiment_analysis'

    # 2) check sentiment_analysis for gold candidates
    for c in SENT_ANLYS_GOLD_CANDIDATES:
        if column_exists(conn, 'sentiment_analysis', c):
            return 'sentiment_analysis', c, 'sentiment_analysis'

    # 3) fallback: if posts.post_sentiment exists and is not null (enum)
    if column_exists(conn, 'posts', 'post_sentiment'):
        return 'posts', 'post_sentiment', 'sentiment_analysis'

    return None, None, 'sentiment_analysis'


def normalize_label(raw) -> Optional[str]:
    if raw is None:
        return None
    if isinstance(raw, (int,)):
        return NUM_TO_CANON.get(raw)
    s = str(raw).strip()
    if not s:
        return None
    # try direct enum-like
    up = s.upper()
    if up in CANONICAL:
        return up
    low = s.lower()
    # english label matches
    for k, v in LABEL_TEXT_TO_CANON.items():
        if low == k:
            return v
    # contains checks
    for k, v in LABEL_TEXT_TO_CANON.items():
        if k in low:
            return v
    # try numeric inside string
    m = re.search(r"\b([0-4])\b", s)
    if m:
        return NUM_TO_CANON.get(int(m.group(1)))
    # last resort: return original upper without spaces
    short = re.sub(r"\W+", "", up)
    if short in CANONICAL:
        return short
    return None


def build_query(conn, gold_table: str, gold_col: str) -> str:
    # Build a safe SQL query: only select predicted columns that exist in the DB.
    # If a predicted column does not exist, select NULL as that alias so pandas/sql won't fail.
    pred_label_exists = column_exists(conn, 'sentiment_analysis', PRED_LABEL_COL)
    pred_class_exists = column_exists(conn, 'sentiment_analysis', PRED_CLASS_COL)

    sel_pred_label = f"sa.{PRED_LABEL_COL} as pred_label" if pred_label_exists else "NULL as pred_label"
    sel_pred_class = f"sa.{PRED_CLASS_COL} as pred_class" if pred_class_exists else "NULL as pred_class"

    # Build WHERE conditions: require at least one of pred_label or pred_class to be non-null
    where_cond = []
    if pred_label_exists:
        where_cond.append(f"sa.{PRED_LABEL_COL} IS NOT NULL")
    if pred_class_exists:
        where_cond.append(f"sa.{PRED_CLASS_COL} IS NOT NULL")
    where_clause = " OR ".join(where_cond) if where_cond else "1=0"

    # Decide how to select the gold column depending on which table it's in
    if gold_table == 'posts':
        gold_select = f"p.{gold_col} as gold_label"
        gold_not_null = f"p.{gold_col} IS NOT NULL"
    elif gold_table == 'sentiment_analysis':
        gold_select = f"sa.{gold_col} as gold_label"
        gold_not_null = f"sa.{gold_col} IS NOT NULL"
    else:
        # fallback: assume posts
        gold_select = f"p.{gold_col} as gold_label"
        gold_not_null = f"p.{gold_col} IS NOT NULL"

    return f"""
        SELECT p.id as post_id, p.content as content,
               {gold_select},
               {sel_pred_label},
               {sel_pred_class}
        FROM posts p
        JOIN sentiment_analysis sa ON sa.post_id = p.id
        WHERE ({where_clause})
          AND {gold_not_null}
    """


def run_evaluation(host: str, port: int, db: str, user: str, password: str, out_csv: str = "evaluation_examples.csv", force_gold: str = None):
    conn = psycopg2.connect(host=host, port=port, database=db, user=user, password=password)
    try:
        if force_gold:
            parts = force_gold.split('.', 1)
            if len(parts) != 2:
                print("--force-gold must be in format table.column")
                sys.exit(1)
            gold_table, gold_col = parts[0], parts[1]
        else:
            gold_table, gold_col, pred_table = detect_ground_truth_columns(conn)
            if not gold_table or not gold_col:
                print("Could not automatically detect a ground-truth column. Please provide a column with human labels (e.g. posts.post_sentiment or sentiment_analysis.manual_label).")
                sys.exit(1)

        print(f"Using ground-truth: {gold_table}.{gold_col}")

        q = build_query(conn, gold_table, gold_col)
        df = pd.read_sql_query(q, conn)
        if df.empty:
            print("No rows found with both gold and predicted labels. Exiting.")
            sys.exit(1)

        # normalize labels
        df['gold_norm'] = df['gold_label'].apply(normalize_label)
        df['pred_norm'] = df.apply(lambda r: normalize_label(r['pred_label']) or normalize_label(r['pred_class']), axis=1)

        df_valid = df.dropna(subset=['gold_norm', 'pred_norm']).copy()
        if df_valid.empty:
            print("After normalization no usable rows remained. Check label formats.")
            print(df[['post_id','gold_label','pred_label','pred_class']].head(30).to_string(index=False))
            sys.exit(1)

        # --- Diagnostics: check how many gold == pred after normalization ---
        df_valid['match'] = df_valid['gold_norm'] == df_valid['pred_norm']
        total = len(df_valid)
        matches = int(df_valid['match'].sum())
        mismatches = total - matches
        match_pct = (matches / total) * 100.0 if total > 0 else 0.0

        print("\n=== Diagnostics ===")
        print(f"Total usable examples: {total}")
        print(f"Exact matches (gold == pred): {matches} ({match_pct:.1f}%)")
        print(f"Mismatches: {mismatches}\n")

        # show some examples for manual inspection
        if matches > 0:
            print("Some matched examples (gold == pred):")
            print(df_valid[df_valid['match']].head(5)[['post_id','content','gold_label','pred_label','gold_norm','pred_norm']].to_string(index=False))
            print()
        if mismatches > 0:
            print("Some mismatched examples (gold != pred):")
            print(df_valid[~df_valid['match']].head(10)[['post_id','content','gold_label','pred_label','gold_norm','pred_norm']].to_string(index=False))
            print()

        y_true = df_valid['gold_norm'].tolist()
        y_pred = df_valid['pred_norm'].tolist()

        labels = CANONICAL

        acc = accuracy_score(y_true, y_pred)
        macro_f1 = f1_score(y_true, y_pred, labels=labels, average='macro')
        weighted_f1 = f1_score(y_true, y_pred, labels=labels, average='weighted')

        print("\n=== Evaluation Summary ===")
        print(f"Examples used: {len(df_valid)}")
        print(f"Accuracy: {acc:.4f}")
        print(f"Macro F1: {macro_f1:.4f}")
        print(f"Weighted F1: {weighted_f1:.4f}\n")

        print("Classification report (per class):\n")
        print(classification_report(y_true, y_pred, labels=labels, digits=4))

        print("Confusion matrix (rows=gold, cols=pred):\n")
        cm = confusion_matrix(y_true, y_pred, labels=labels)
        print(pd.DataFrame(cm, index=labels, columns=labels))

        # export CSV with examples used
        df_valid[['post_id','content','gold_label','pred_label','pred_class','gold_norm','pred_norm']].to_csv(out_csv, index=False, quoting=csv.QUOTE_NONNUMERIC)
        print(f"\nExported examples to {out_csv}")

    finally:
        conn.close()


if __name__ == '__main__':
    # try to read application.properties first
    jdbc_url, db_user, db_pass, props_path = parse_application_properties(APP_PROPS_PATH)

    env_host = os.environ.get('DB_HOST')
    env_port = os.environ.get('DB_PORT')
    env_db = os.environ.get('DB_NAME')
    env_user = os.environ.get('DB_USER')
    env_pass = os.environ.get('DB_PASS')

    if env_host and env_db and env_user:
        host = env_host; port = int(env_port or 5432); db = env_db; user = env_user; password = env_pass or ''
    elif jdbc_url and db_user is not None:
        host, port, db = parse_jdbc_url(jdbc_url)
        user = db_user
        password = db_pass or ''
    else:
        print("No DB configuration found: set DB_HOST/DB_NAME/DB_USER/DB_PASS or fill application.properties.")
        sys.exit(1)

    print(f"Connecting to {host}:{port}/{db} as {user}")
    # CLI: allow forcing which column to use as gold (format: table.column)
    parser = argparse.ArgumentParser(description="Evaluate sentiment predictions against gold labels in DB")
    parser.add_argument('--force-gold', type=str, help='Force the gold column to use (format: table.column), e.g. sentiment_analysis.manual_label or posts.post_sentiment')
    parser.add_argument('--out-csv', type=str, default='evaluation_examples.csv', help='Output CSV file')
    args = parser.parse_args()

    # If force-gold provided, validate existence
    if args.force_gold:
        parts = args.force_gold.split('.', 1)
        if len(parts) != 2:
            print("--force-gold must be in format table.column")
            sys.exit(1)
        f_table, f_col = parts[0], parts[1]
        conn_check = psycopg2.connect(host=host, port=port, database=db, user=user, password=password)
        try:
            if not column_exists(conn_check, f_table, f_col):
                print(f"Forced gold column {args.force_gold} not found in DB.")
                sys.exit(1)
        finally:
            conn_check.close()
        # Run evaluation using forced column
        run_evaluation(host, port, db, user, password, out_csv=args.out_csv, force_gold=args.force_gold)
    else:
        run_evaluation(host, port, db, user, password, out_csv=args.out_csv, force_gold=None)

