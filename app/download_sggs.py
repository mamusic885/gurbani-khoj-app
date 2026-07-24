import urllib.request
import json
import sqlite3
import os
import time
import re
from concurrent.futures import ThreadPoolExecutor

os.makedirs('app/src/main/assets/databases', exist_ok=True)
db_path = 'app/src/main/assets/databases/sggs_database.db'

if os.path.exists(db_path):
    os.remove(db_path)

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

cursor.execute('''
CREATE TABLE IF NOT EXISTS sggs_verses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ang INTEGER NOT NULL,
    lineIndex INTEGER NOT NULL,
    gurmukhi TEXT NOT NULL,
    transliteration TEXT,
    translation TEXT,
    firstLetters TEXT,
    firstLettersAscii TEXT
)
''')

cursor.execute('CREATE INDEX IF NOT EXISTS idx_ang ON sggs_verses(ang);')
cursor.execute('CREATE INDEX IF NOT EXISTS idx_first_letters ON sggs_verses(firstLetters);')
cursor.execute('CREATE INDEX IF NOT EXISTS idx_first_letters_ascii ON sggs_verses(firstLettersAscii);')

print("Starting fetch of 1430 Angs...")

def get_gurmukhi_first_letters(text):
    # Remove punctuation
    cleaned = re.sub(r'[\u0964\u0965\|\:\[\]\(\)\{\}\,\.\-\?\!]', '', text)
    words = cleaned.split()
    firsts = []
    for w in words:
        if w:
            firsts.append(w[0])
    return "".join(firsts)

def get_ascii_first_letters(text):
    cleaned = re.sub(r'[^a-zA-Z\s]', '', text)
    words = cleaned.split()
    firsts = []
    for w in words:
        if w:
            firsts.append(w[0].lower())
    return "".join(firsts)

def fetch_ang(ang):
    url = f'https://api.gurbaninow.com/v2/ang/{ang}'
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=12) as resp:
                data = json.loads(resp.read().decode())
                page = data.get('page', [])
                verses = []
                for idx, item in enumerate(page):
                    line_data = item.get('line', {})
                    gurmukhi = line_data.get('gurmukhi', {}).get('unicode', '')
                    if not gurmukhi:
                        continue
                    
                    translit = line_data.get('transliteration', {}).get('english', {}).get('text', '')
                    translation = line_data.get('translation', {}).get('english', {}).get('default', '')
                    
                    # First letters
                    fl_unicode = line_data.get('firstletters', {}).get('unicode', '')
                    if not fl_unicode:
                        fl_unicode = get_gurmukhi_first_letters(gurmukhi)
                        
                    fl_ascii = get_ascii_first_letters(translit)
                    
                    verses.append((
                        ang,
                        idx + 1,
                        gurmukhi,
                        translit,
                        translation,
                        fl_unicode,
                        fl_ascii
                    ))
                return ang, verses
        except Exception as e:
            time.sleep(1)
    return ang, []

all_verses = []
start_time = time.time()

with ThreadPoolExecutor(max_workers=30) as executor:
    futures = [executor.submit(fetch_ang, a) for a in range(1, 1431)]
    for future in futures:
        ang, verses = future.result()
        all_verses.extend(verses)
        if ang % 100 == 0 or ang == 1430:
            print(f"Fetched up to Ang {ang}... Total verses fetched so far: {len(all_verses)}")

print(f"Total verses to insert: {len(all_verses)}")

cursor.executemany('''
INSERT INTO sggs_verses (ang, lineIndex, gurmukhi, transliteration, translation, firstLetters, firstLettersAscii)
VALUES (?, ?, ?, ?, ?, ?, ?)
''', all_verses)

conn.commit()
conn.close()

db_size = os.path.getsize(db_path) / (1024 * 1024)
print(f"SUCCESS: Saved {len(all_verses)} verses to {db_path} ({db_size:.2f} MB) in {time.time()-start_time:.2f} seconds!")
