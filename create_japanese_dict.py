import sqlite3
import os

db_path = r'c:\Users\PCD\Desktop\malang_key\app\src\main\assets\ime\languagepack\org.florisboard.japanesepack\japanese_dict.sqlite3'

if os.path.exists(db_path):
    os.remove(db_path)

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

cursor.execute('''
CREATE TABLE dictionary (
    id INTEGER PRIMARY KEY,
    reading TEXT NOT NULL,
    word TEXT NOT NULL,
    frequency INTEGER DEFAULT 0
)
''')

cursor.execute('CREATE INDEX idx_reading ON dictionary(reading)')

# Insert some mock test data
mock_data = [
    ('あ', '亜', 100),
    ('あか', '赤', 200),
    ('あかじ', '赤字', 300),
    ('あかじ', '赤地', 150),
    ('わたし', '私', 500),
    ('あなた', '貴方', 300),
    ('くるま', '車', 400),
    ('きょう', '今日', 500),
    ('あした', '明日', 400),
    ('きのう', '昨日', 400),
    ('おはよう', 'お早う', 200),
    ('ありがとう', '有難う', 200),
]

cursor.executemany('INSERT INTO dictionary (reading, word, frequency) VALUES (?, ?, ?)', mock_data)

conn.commit()
conn.close()

print(f"Successfully created mock Japanese dictionary at: {db_path}")
