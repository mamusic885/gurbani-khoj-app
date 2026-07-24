import json, re

def gurmukhi_char_to_roman(ch):
    norm_map = {
        "ੴ": "i",
        "ੳ": "a", "ਅ": "a", "ੲ": "a", "ਆ": "a", "ਇ": "i", "ਈ": "i", "ਉ": "u", "ਊ": "u", "ਏ": "e", "ਐ": "a", "ਓ": "o", "ਔ": "a",
        "ਕ": "k", "ਖ": "k", "ਖ਼": "k", "ਗ": "g", "ਘ": "g", "ਗ਼": "g", "ਙ": "n",
        "ਚ": "c", "ਛ": "c", "ਜ": "j", "ਝ": "j", "ਜ਼": "z", "ਞ": "n",
        "ਟ": "t", "ਠ": "t", "ਡ": "d", "ਢ": "d", "ਣ": "n",
        "ਤ": "t", "ਥ": "t", "ਦ": "d", "ਧ": "d", "ਨ": "n",
        "ਪ": "p", "ਫ": "p", "ਫ਼": "f", "ਬ": "b", "ਭ": "b", "ਮ": "m",
        "ਯ": "y", "ਰ": "r", "ੜ": "r", "ਲ": "l", "ਲ਼": "l", "ਵ": "v", "ਸ਼": "s", "ਸ": "s", "ਹ": "h"
    }
    return norm_map.get(ch, "")

class WordToken:
    def __init__(self, word, start, end, exact_g, norm_g, roman):
        self.word = word
        self.start = start
        self.end = end
        self.exact_g = exact_g
        self.norm_g = norm_g
        self.roman = roman

def parse_line(line):
    tokens = []
    i = 0
    l = len(line)
    norm_vowel = {
        "ਆ": "ਅ", "ਇ": "ਅ", "ਈ": "ਅ", "ਏ": "ਅ", "ਐ": "ਅ",
        "ਉ": "ਅ", "ਊ": "ਅ", "ਓ": "ਅ", "ਔ": "ਅ", "ੳ": "ਅ", "ੲ": "ਅ"
    }
    while i < l:
        while i < l and (line[i].isspace() or line[i] in "॥|,.?()0123456789੦੧੨੩੪੫੬੭੮੯-\"'"):
            i += 1
        if i >= l: break
        st = i
        while i < l and not (line[i].isspace() or line[i] in "॥|,.?()0123456789੦੧੨੩੪੫੬੭੮੯-\"'"):
            i += 1
        w = line[st:i]
        ch = w[0]
        tokens.append(WordToken(w, st, i, ch, norm_vowel.get(ch, ch), gurmukhi_char_to_roman(ch)))
    return tokens

line = "ਸੁਣੀ ਅਰਦਾਸਿ ਸੁਆਮੀ ਮੇਰੇ ਸਰਬ ਕਲਾ ਬਣਿ ਆਈ"
tokens = parse_line(line)
roman_fl = "".join([t.roman for t in tokens])
g_fl = "".join([t.exact_g for t in tokens])

print("Line:", line)
print("Tokens count:", len(tokens))
print("Roman FL:", roman_fl)
print("Gurmukhi FL:", g_fl)

def match_fl(tokens, q):
    clean = q.replace(" ", "").lower()
    roman_fl = "".join([t.roman for t in tokens])
    idx = roman_fl.find(clean)
    if idx >= 0:
        start_char = tokens[idx].start
        end_char = tokens[idx + len(clean) - 1].end - 1
        return True, (start_char, end_char)
    qi = 0
    s_t = -1
    e_t = -1
    for ti, t in enumerate(tokens):
        if t.roman == clean[qi]:
            if qi == 0: s_t = ti
            qi += 1
            if qi == len(clean):
                e_t = ti
                return True, (tokens[s_t].start, tokens[e_t].end - 1)
    return False, None

print("saskba match:", match_fl(tokens, "saskba"))
print("s a s m s k b a match:", match_fl(tokens, "s a s m s k b a"))
