import urllib.request
import json
import sqlite3
import os
import time
import re
from concurrent.futures import ThreadPoolExecutor

db_dir = '/app/src/main/assets/databases'
os.makedirs(db_dir, exist_ok=True)
dest_db_path = os.path.join(db_dir, 'sggs_database.db')
tmp_db_path = '/tmp/sggs_shabads_tmp.db'

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
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode())
                page = data.get('page', [])
                return ang, page
        except Exception:
            time.sleep(0.5)
    return ang, []

def build_database(start_ang=1, end_ang=1430):
    print(f"Fetching Angs {start_ang} to {end_ang} from GurbaniNow API...")
    t0 = time.time()
    
    ang_data_map = {}
    with ThreadPoolExecutor(max_workers=40) as executor:
        results = executor.map(fetch_ang, range(start_ang, end_ang + 1))
        for ang, page in results:
            ang_data_map[ang] = page
            
    t1 = time.time()
    print(f"Downloaded {len(ang_data_map)} Angs in {t1-t0:.2f} seconds.")
    
    # Group lines into Shabads
    shabads = []
    current_shabad_id = None
    current_shabad_lines = []
    
    for ang in range(start_ang, end_ang + 1):
        page = ang_data_map.get(ang, [])
        if not page:
            continue
            
        for item in page:
            line_data = item.get('line', {})
            gurmukhi = line_data.get('gurmukhi', {}).get('unicode', '')
            if not gurmukhi:
                continue
                
            shabad_id = line_data.get('shabadid', str(ang))
            translit = line_data.get('transliteration', {}).get('english', {}).get('text', '')
            translation = line_data.get('translation', {}).get('english', {}).get('default', '')
            raag = line_data.get('raag', {}).get('gurmukhi', '')
            writer = line_data.get('writer', {}).get('gurmukhi', '')
            
            line_info = {
                'ang': ang,
                'gurmukhi': gurmukhi,
                'transliteration': translit,
                'translation': translation,
                'raag': raag,
                'writer': writer,
                'shabad_id': shabad_id
            }
            
            if current_shabad_id is None:
                current_shabad_id = shabad_id
                current_shabad_lines = [line_info]
            elif shabad_id == current_shabad_id:
                current_shabad_lines.append(line_info)
            else:
                shabads.append(current_shabad_lines)
                current_shabad_id = shabad_id
                current_shabad_lines = [line_info]
                
    if current_shabad_lines:
        shabads.append(current_shabad_lines)
        
    print(f"Grouped into {len(shabads)} complete Shabads!")
    
    # Save to SQLite database
    if os.path.exists(tmp_db_path):
        os.remove(tmp_db_path)
        
    conn = sqlite3.connect(tmp_db_path)
    cursor = conn.cursor()
    cursor.execute('PRAGMA journal_mode = OFF;')
    
    # Room Entity schema for SggsShabadEntity and backward compatible sggs_verses if needed
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
    
    # Also create sggs_verses table representing lines, mapped to shabadId and ang
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
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_verses_firstLettersAscii ON sggs_verses(firstLettersAscii);')

    shabad_rows = []
    verse_rows = []
    
    for s_idx, s_lines in enumerate(shabads):
        first_line = s_lines[0]
        s_id = first_line['shabad_id']
        s_ang = first_line['ang']
        s_raag = first_line['raag'] or ""
        s_writer = first_line['writer'] or ""
        s_title = first_line['gurmukhi']
        
        gurmukhi_lines = [l['gurmukhi'] for l in s_lines]
        translit_lines = [l['transliteration'] for l in s_lines]
        translation_lines = [l['translation'] for l in s_lines]
        
        full_gurmukhi = "\n".join(gurmukhi_lines)
        full_translit = "\n".join(translit_lines)
        full_translation = "\n".join(translation_lines)
        
        # Combine first letters for all lines in the Shabad
        fl_uni_parts = [get_gurmukhi_first_letters(line) for line in gurmukhi_lines]
        fl_asc_parts = [get_ascii_first_letters(line) for line in translit_lines if line]
        
        full_fl_uni = " ".join(fl_uni_parts)
        full_fl_asc = " ".join(fl_asc_parts)
        
        shabad_rows.append((
            s_id,
            s_ang,
            s_raag,
            s_writer,
            s_title,
            full_gurmukhi,
            full_translit,
            full_translation,
            full_fl_uni,
            full_fl_asc
        ))
        
        for l_idx, l in enumerate(s_lines):
            fl_u = get_gurmukhi_first_letters(l['gurmukhi'])
            fl_a = get_ascii_first_letters(l['transliteration'])
            verse_rows.append((
                l['ang'],
                l_idx + 1,
                s_id,
                l['gurmukhi'],
                l['transliteration'],
                l['translation'],
                fl_u,
                fl_a
            ))
            
    cursor.executemany('''
    INSERT INTO sggs_shabads (shabadId, ang, raag, writer, title, gurmukhi, transliteration, translation, firstLetters, firstLettersAscii)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ''', shabad_rows)
    
    cursor.executemany('''
    INSERT INTO sggs_verses (ang, lineIndex, shabadId, gurmukhi, transliteration, translation, firstLetters, firstLettersAscii)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    ''', verse_rows)
    
    conn.commit()
    conn.close()
    
    if os.path.exists(dest_db_path):
        os.remove(dest_db_path)
    import shutil
    shutil.copyfile(tmp_db_path, dest_db_path)
    
    db_size = os.path.getsize(dest_db_path) / (1024 * 1024)
    print(f"SUCCESS! Database created at {dest_db_path} ({db_size:.2f} MB)")
    print(f"Total Shabads: {len(shabad_rows)}, Total Verses: {len(verse_rows)}")

if __name__ == '__main__':
    build_database(1, 1430)
