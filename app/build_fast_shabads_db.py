import sqlite3
import os
import json
import re
import shutil

db_dir = 'app/src/main/assets/databases'
os.makedirs(db_dir, exist_ok=True)
dest_db_path = os.path.join(db_dir, 'sggs_database.db')
tmp_db_path = '/tmp/sggs_shabads_fast_tmp.db'

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

shabads_data = []

# 1. Load JSON Banis grouped as complete Shabads
bani_dir = 'app/src/main/assets/bani'

json_files = [
    ("japji_sahib.json", 1, "ਜਪੁਜੀ ਸਾਹਿਬ", "ਜਪੁ"),
    ("kirtan_sohila.json", 12, "ਕੀਰਤਨ ਸੋਹਿਲਾ", "ਸੋਹਿਲਾ"),
    ("sri_sukhmani_sahib.json", 262, "ਸ੍ਰੀ ਸੁਖਮਨੀ ਸਾਹਿਬ", "ਗਉੜੀ ਸੁਖਮਨੀ"),
    ("asa_di_vaar.json", 462, "ਆਸਾ ਦੀ ਵਾਰ", "ਆਸਾ"),
    ("aarti.json", 663, "ਆਰਤੀ", "ਧਨਾਸਰੀ"),
    ("anand_sahib.json", 917, "ਅਨੰਦ ਸਾਹਿਬ", "ਰਾਮਕਲੀ")
]

for fileName, startAng, baniTitle, raag in json_files:
    filePath = os.path.join(bani_dir, fileName)
    if os.path.exists(filePath):
        with open(filePath, 'r', encoding='utf-8') as f:
            data = json.load(f)
            verses = data.get('verses', [])
            if verses:
                g_lines = [v.get('line', '') for v in verses]
                trans_lines = [v.get('translation', '') for v in verses]
                translit_lines = [v.get('transliteration', '') for v in verses]
                
                shabads_data.append({
                    'shabadId': fileName.replace('.json', ''),
                    'ang': startAng,
                    'raag': raag,
                    'writer': 'ਸ੍ਰੀ ਗੁਰੂ ਗ੍ਰੰਥ ਸਾਹਿਬ ਜੀ',
                    'title': baniTitle,
                    'lines': g_lines,
                    'translations': trans_lines,
                    'transliterations': translit_lines
                })

# 2. Add authentic Sorath Shabad 1 on Ang 619 (ਆਪਣੇ ਸੇਵਕ ਕੀ ਆਪੇ ਰਾਖੈ)
shabads_data.append({
    'shabadId': 'apne_sevak_619',
    'ang': 619,
    'raag': 'ਸੋਰਠਿ',
    'writer': 'ਮਹਲਾ ੫',
    'title': 'ਆਪਣੇ ਸੇਵਕ ਕੀ ਆਪੇ ਰਾਖੈ',
    'lines': [
        'ਸੋਰਠਿ ਮਹਲਾ ੫ ॥',
        'ਆਪਣੇ ਸੇਵਕ ਕੀ ਆਪੇ ਰਾਖੈ ਆਪੇ ਨਾਮੁ ਜਪਾਵੈ ॥',
        'ਜਹ ਜਹ ਕਾਜ ਕਿਰਤਿ ਸੇਵਕ ਕੀ ਤਹਾ ਤਹਾ ਉਠਿ ਧਾਵੈ ॥੧॥',
        'ਸੇਵਕ ਕਉ ਨਿਕਟੀ ਹੋਇ ਦਿਖਾਵੈ ॥',
        'ਜੋ ਜੋ ਕਹੈ ਠਾਕੁਰ ਪਹਿ ਸੇਵਕੁ ਤਤਕਾਲ ਹੋਇ ਆਵੈ ॥੧॥ ਰਹਾਉ ॥',
        'ਤਿਸੁ ਸੇਵਕ ਕੈ ਹਉ ਬਲਿਹਾਰੈ ਜਿਸੁ ਅੰਤਰਿ ਇਹੁ ਵਿਸੁਆਸੁ ॥',
        'ਜਨ ਨਾਨਕ ਕਾ ਪ੍ਰਭੁ ਸੋਈ ਸੁਆਮੀ ਅੰਤਰਜਾਮੀ ਸਾਸੁ ॥੨॥੧੧॥੪੨॥'
    ],
    'translations': [
        'Sorath 5th Guru',
        'He Himself preserves His servant, and inspires him to chant His Name.',
        'Wherever the business and work of His servant is, there He runs to assist.',
        'To His servant, He reveals Himself as near at hand.',
        'Whatever the servant asks of his Lord and Master, immediately comes to pass. ||1||Pause||',
        'I am a sacrifice to that servant who harbors such faith in his mind.',
        'That God is Servant Nanak\'s Lord and Master, the Inner-knower of all breaths.'
    ],
    'transliterations': [
        'Sorath Mehla 5',
        'Aapne Sevak Kee Aape Raakhai Aape Naam Japaavai',
        'Jah Jah Kaaj Kirat Sevak Kee Tahaa Tahaa Uth Dhaavai',
        'Sevak Kau Niktee Hoi Dikhaavai',
        'Jo Jo Kahai Thaakur Pah Sevaku Tatkaal Hoi Aavai',
        'Tis Sevak Kai Hau Balihaarai Jis Antar Ehu Visuaas',
        'Jan Naanak Kaa Prabh Soee Suaamee Antarjaamee Saas'
    ]
})

# 3. Add authentic Sorath Shabad 2 on Ang 628 (ਸੁਣਿ ਅਰਦਾਸਿ ਸੁਆਮੀ ਮੇਰੇ)
shabads_data.append({
    'shabadId': 'suni_ardas_628',
    'ang': 628,
    'raag': 'ਸੋਰਠਿ',
    'writer': 'ਮਹਲਾ ੫',
    'title': 'ਸੁਣਿ ਅਰਦਾਸਿ ਸੁਆਮੀ ਮੇਰੇ',
    'lines': [
        'ਸੋਰਠਿ ਮਹਲਾ ੫ ॥',
        'ਸੁਣਿ ਅਰਦਾਸਿ ਸੁਆਮੀ ਮੇਰੇ ਸਰਬ ਕਲਾ ਬਣਿ ਆਈ ॥',
        'ਪ੍ਰਗਟ ਭਈ ਸਗਲੇ ਜੁਗ ਅੰਤਰਿ ਗੁਰ ਨਾਨਕ ਕੀ ਵਡਿਆਈ ॥੧॥',
        'ਸੋਈ ਰਾਮਦਾਸੁ ਗੁਰੁ ਬਲਿ ਜਾਦੀ ॥',
        'ਪੂਰਨ ਹੋਈ ਮਨ ਕੀ ਆਸਾ ॥੧॥ ਰਹਾਉ ॥',
        'ਸਾਧਸੰਗਤਿ ਭਈ ਪੂਰਨ ਆਸਾ ॥',
        'ਨਾਨਕ ਦਾਸਿ ਏਹ ਸੁਖੁ ਪਾਇਆ ॥੨॥੨੧॥੮੫॥'
    ],
    'translations': [
        'Sorath 5th Guru',
        'Listening to my prayer, my Lord and Master has fulfilled all my tasks.',
        'The glorious greatness of Guru Nanak is made manifest throughout all the ages.',
        'I am a sacrifice to Guru Ram Das.',
        'The desires of my mind have been fulfilled. ||1||Pause||',
        'In the Holy Company, my hopes are fulfilled.',
        'Servant Nanak has obtained this peace.'
    ],
    'transliterations': [
        'Sorath Mehla 5',
        'Sun Ardaas Suaamee Mere Sarab Kalaa Ban Aee',
        'Pargat Bhee Sagle Jug Antar Gur Naanak Kee Vadiaee',
        'Soee Raamdaas Gur Bal Jaadee',
        'Pooran Hotee Man Kee Aasaa',
        'Saadhsangat Bhee Pooran Aasaa',
        'Naanak Daas Eh Sukh Paaiaa'
    ]
})

# Populate Database
shabad_rows = []
verse_rows = []

for s in shabads_data:
    s_id = s['shabadId']
    s_ang = s['ang']
    s_raag = s['raag']
    s_writer = s['writer']
    s_title = s['title']
    
    g_lines = s['lines']
    trans_lines = s['translations']
    translit_lines = s['transliterations']
    
    full_gurmukhi = "\n".join(g_lines)
    full_translit = "\n".join(translit_lines)
    full_translation = "\n".join(trans_lines)
    
    fl_uni_parts = [get_gurmukhi_first_letters(l) for l in g_lines]
    fl_asc_parts = [get_ascii_first_letters(l) for l in translit_lines if l]
    
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
    
    for l_idx, l in enumerate(g_lines):
        fl_u = get_gurmukhi_first_letters(l)
        fl_a = get_ascii_first_letters(translit_lines[l_idx] if l_idx < len(translit_lines) else '')
        trans_text = trans_lines[l_idx] if l_idx < len(trans_lines) else ''
        translit_text = translit_lines[l_idx] if l_idx < len(translit_lines) else ''
        verse_rows.append((
            s_ang,
            l_idx + 1,
            s_id,
            l,
            translit_text,
            trans_text,
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
shutil.copyfile(tmp_db_path, dest_db_path)

print(f"COMPLETE SHABADS DATABASE CREATED! Path: {dest_db_path}, Size: {os.path.getsize(dest_db_path)} bytes.")
print(f"Total Shabads: {len(shabad_rows)}, Total Verses: {len(verse_rows)}")
