import sqlite3
import os
import json
import re
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor

tmp_db_path = '/tmp/sggs_full_1430.db'
dest_db_path = 'app/src/main/assets/databases/sggs_database.db'
os.makedirs(os.path.dirname(dest_db_path), exist_ok=True)

def normalize_gurmukhi_char(c):
    if c in ['ਆ', 'ਇ', 'ਈ', 'ਏ', 'ਐ', 'ਉ', 'ਊ', 'ਓ', 'ਔ', 'ੳ', 'ੲ']:
        return 'ਅ'
    return c

def get_gurmukhi_first_letters(text):
    cleaned = re.sub(r'[\u0964\u0965\|\:\[\]\(\)\{\}\,\.\-\?\!\d]', '', text)
    words = cleaned.split()
    firsts_exact = []
    firsts_norm = []
    for w in words:
        if w:
            firsts_exact.append(w[0])
            firsts_norm.append(normalize_gurmukhi_char(w[0]))
    exact_str = "".join(firsts_exact)
    norm_str = "".join(firsts_norm)
    if exact_str == norm_str:
        return exact_str
    return f"{exact_str} {norm_str}"

def get_ascii_first_letters(translit):
    cleaned = re.sub(r'[^a-zA-Z\s]', '', translit)
    words = cleaned.split()
    firsts = []
    for w in words:
        if w:
            firsts.append(w[0].lower())
    return "".join(firsts)

def fetch_ang(ang):
    url = f'https://api.gurbaninow.com/v2/ang/{ang}'
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode())
                return ang, data.get('page', [])
        except Exception:
            time.sleep(0.3 * (attempt + 1))
    return ang, []

print("Starting fetch of 1430 Angs from GurbaniNow API...")
t0 = time.time()

# Use 40 worker threads for fast parallel fetching
with ThreadPoolExecutor(max_workers=40) as executor:
    results = list(executor.map(fetch_ang, range(1, 1431)))

t1 = time.time()
print(f"Fetched 1430 Angs in {t1 - t0:.2f} seconds.")

# Map ang -> lines
ang_data_map = {}
success_count = 0
for ang, page_lines in results:
    if page_lines:
        success_count += 1
        ang_data_map[ang] = page_lines

print(f"Successfully retrieved {success_count} / 1430 Angs.")

# Process lines and group into Shabads
# In GurbaniNow API, lines belonging to the same Shabad share the same 'shabadid'
shabads_dict = {} # shabadid -> dict of shabad properties & lines
all_verses = []

for ang in range(1, 1431):
    lines = ang_data_map.get(ang, [])
    for line_idx, line_item in enumerate(lines):
        l_info = line_item.get('line', {})
        s_id = str(l_info.get('shabadid', ''))
        if not s_id or s_id == 'None':
            s_id = f"ang_{ang}_line_{line_idx+1}"

        g_obj = l_info.get('gurmukhi', {})
        gurmukhi_text = g_obj.get('unicode', '') if isinstance(g_obj, dict) else str(g_obj)

        t_obj = l_info.get('transliteration', {})
        translit_text = ""
        if isinstance(t_obj, dict):
            eng_t = t_obj.get('english', {})
            if isinstance(eng_t, dict):
                translit_text = eng_t.get('text', '')

        tr_obj = l_info.get('translation', {})
        translation_text = ""
        if isinstance(tr_obj, dict):
            eng_tr = tr_obj.get('english', {})
            if isinstance(eng_tr, dict):
                translation_text = eng_tr.get('default', '')

        r_obj = l_info.get('raag', {})
        raag_text = r_obj.get('unicode', '') if isinstance(r_obj, dict) else str(r_obj)

        w_obj = l_info.get('writer', {})
        writer_text = w_obj.get('unicode', '') if isinstance(w_obj, dict) else str(w_obj)

        fl_u = get_gurmukhi_first_letters(gurmukhi_text)
        fl_a = get_ascii_first_letters(translit_text)

        all_verses.append((
            ang,
            line_idx + 1,
            s_id,
            gurmukhi_text,
            translit_text,
            translation_text,
            fl_u,
            fl_a
        ))

        if s_id not in shabads_dict:
            shabads_dict[s_id] = {
                'shabadId': s_id,
                'ang': ang,
                'raag': raag_text,
                'writer': writer_text,
                'gurmukhi_lines': [gurmukhi_text],
                'translit_lines': [translit_text],
                'translation_lines': [translation_text]
            }
        else:
            shabads_dict[s_id]['gurmukhi_lines'].append(gurmukhi_text)
            shabads_dict[s_id]['translit_lines'].append(translit_text)
            shabads_dict[s_id]['translation_lines'].append(translation_text)

print(f"Grouped into {len(shabads_dict)} distinct Shabads and {len(all_verses)} total verses.")

# Prepare shabad database rows
shabad_rows = []
for s_id, s_data in shabads_dict.items():
    g_full = "\n".join(s_data['gurmukhi_lines'])
    translit_full = "\n".join(s_data['translit_lines'])
    translation_full = "\n".join(s_data['translation_lines'])

    # Title is first non-empty line
    title = s_data['gurmukhi_lines'][0] if s_data['gurmukhi_lines'] else ""

    fl_u_parts = [get_gurmukhi_first_letters(l) for l in s_data['gurmukhi_lines']]
    fl_a_parts = [get_ascii_first_letters(l) for l in s_data['translit_lines']]

    fl_u_full = " ".join(fl_u_parts)
    fl_a_full = " ".join(fl_a_parts)

    shabad_rows.append((
        s_data['shabadId'],
        s_data['ang'],
        s_data['raag'],
        s_data['writer'],
        title,
        g_full,
        translit_full,
        translation_full,
        fl_u_full,
        fl_a_full
    ))

# Create SQLite Database
if os.path.exists(tmp_db_path):
    os.remove(tmp_db_path)

conn = sqlite3.connect(tmp_db_path)
cursor = conn.cursor()
cursor.execute('PRAGMA journal_mode = OFF;')

cursor.execute('''
CREATE TABLE IF NOT EXISTS sggs_shabads (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    shabadId TEXT NOT NULL,
    ang INTEGER NOT NULL,
    raag TEXT NOT NULL,
    writer TEXT NOT NULL,
    title TEXT NOT NULL,
    gurmukhi TEXT NOT NULL,
    transliteration TEXT NOT NULL,
    translation TEXT NOT NULL,
    firstLetters TEXT NOT NULL,
    firstLettersAscii TEXT NOT NULL
);
''')

cursor.execute('CREATE INDEX IF NOT EXISTS idx_shabads_ang ON sggs_shabads(ang);')
cursor.execute('CREATE INDEX IF NOT EXISTS idx_shabads_shabadId ON sggs_shabads(shabadId);')
cursor.execute('CREATE INDEX IF NOT EXISTS idx_shabads_firstLetters ON sggs_shabads(firstLetters);')
cursor.execute('CREATE INDEX IF NOT EXISTS idx_shabads_firstLettersAscii ON sggs_shabads(firstLettersAscii);')

cursor.execute('''
CREATE TABLE IF NOT EXISTS sggs_verses (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    ang INTEGER NOT NULL,
    lineIndex INTEGER NOT NULL,
    shabadId TEXT NOT NULL,
    gurmukhi TEXT NOT NULL,
    transliteration TEXT NOT NULL,
    translation TEXT NOT NULL,
    firstLetters TEXT NOT NULL,
    firstLettersAscii TEXT NOT NULL
);
''')

cursor.execute('CREATE INDEX IF NOT EXISTS idx_verses_ang ON sggs_verses(ang);')
cursor.execute('CREATE INDEX IF NOT EXISTS idx_verses_shabadId ON sggs_verses(shabadId);')
cursor.execute('CREATE INDEX IF NOT EXISTS idx_verses_firstLetters ON sggs_verses(firstLetters);')

cursor.executemany('''
INSERT INTO sggs_shabads (shabadId, ang, raag, writer, title, gurmukhi, transliteration, translation, firstLetters, firstLettersAscii)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
''', shabad_rows)

cursor.executemany('''
INSERT INTO sggs_verses (ang, lineIndex, shabadId, gurmukhi, transliteration, translation, firstLetters, firstLettersAscii)
VALUES (?, ?, ?, ?, ?, ?, ?, ?)
''', all_verses)

conn.commit()
conn.close()

import shutil
if os.path.exists(dest_db_path):
    os.remove(dest_db_path)
shutil.copyfile(tmp_db_path, dest_db_path)

print(f"OFFLINE DATABASE CREATED SUCCESSFULLY AT {dest_db_path}!")
print(f"Database Size: {os.path.getsize(dest_db_path)} bytes.")
print(f"Total Shabads: {len(shabad_rows)}, Total Verses: {len(all_verses)}.")
