"""Llama 3.2 (via Ollama): paragraph generator + short summary + keywords +
hard words with Hindi/English meanings. Optimised for speed (streaming,
one analysis call, model kept loaded in memory)."""
import json, re
import ollama

DEFAULT_MODEL = "llama3.2"
KEEP_ALIVE = "30m"          # keep model in RAM -> no reload delay
CHUNK_CHARS = 5000
TOKENS_PER_WORD = {"English": 1.5, "Hindi": 6.0, "Hinglish": 2.5}


def count_words(text: str) -> int:
    return len(text.split())


def short_words_for(words: int) -> int:
    """Short-summary length grows with the paragraph length (30..200 words)."""
    return max(30, min(200, words // 5))


def _trim(text: str, limit: int) -> str:
    w = text.split()
    if len(w) <= limit:
        return text
    cut = " ".join(w[:limit])
    m = max(cut.rfind("."), cut.rfind("।"), cut.rfind("!"), cut.rfind("?"))
    return cut[: m + 1] if m > len(cut) * 0.6 else cut


def _opts(words: int, lang: str, extra_ctx_tokens: int = 0, temp: float = 0.6) -> dict:
    tpw = TOKENS_PER_WORD.get(lang, 2.0)
    num_predict = int(words * tpw) + 80
    num_ctx = min(8192, max(2048, num_predict + extra_ctx_tokens + 600))
    return {"temperature": temp, "num_predict": num_predict, "num_ctx": num_ctx}


# ---------------------------------------------------------------- paragraph
def stream_paragraph(source: str, words: int, lang: str, model: str, is_topic: bool):
    """Yields text chunks so the UI can show the paragraph while it is written."""
    system = (f"You are an expert writer. Write ONLY in {lang}. "
              "Output plain flowing prose: no headings, no bullet points, no preface.")
    if is_topic:
        user = (f"Write an informative paragraph about the topic below in about {words} words. "
                f"Do not stop before reaching {words} words.\n\nTOPIC: {source}")
    else:
        user = (f"Write a clear summary of the document below in about {words} words. "
                f"Do not stop before reaching {words} words.\n\nDOCUMENT:\n{source}")
    stream = ollama.chat(
        model=model, stream=True, keep_alive=KEEP_ALIVE,
        messages=[{"role": "system", "content": system}, {"role": "user", "content": user}],
        options=_opts(words, lang, extra_ctx_tokens=len(source) // 3),
    )
    for chunk in stream:
        yield chunk["message"]["content"]


def fix_length(text: str, words: int, lang: str, model: str) -> str:
    """Optional: one extra pass so the length matches the request (slower)."""
    n = count_words(text)
    if int(words * 0.92) <= n <= int(words * 1.08):
        return text
    action = "Expand it with more relevant detail" if n < words else "Shorten it"
    r = ollama.chat(
        model=model, keep_alive=KEEP_ALIVE,
        messages=[{"role": "user", "content":
                   f"This text has {n} words. {action} so it has about {words} words. "
                   f"Keep the language ({lang}) and meaning. Return only the text.\n\n{text}"}],
        options=_opts(words, lang, extra_ctx_tokens=n * 2, temp=0.4),
    )
    return _trim(r["message"]["content"].strip(), int(words * 1.08))


# ---------------------------------------------------------------- analysis
def _extract_json(raw: str) -> dict:
    try:
        return json.loads(raw)
    except Exception:
        m = re.search(r"\{.*\}", raw, re.S)
        try:
            return json.loads(m.group(0)) if m else {}
        except Exception:
            return {}


def analyze(text: str, lang: str, model: str, short_words: int, with_points=False) -> dict:
    """ONE call -> short summary + keywords + hard words (+ key points)."""
    pts = ('"key_points": ["5-7 most important points, one sentence each"],\n ' if with_points else "")
    prompt = (
        "Read the TEXT and return ONLY valid JSON with exactly these keys:\n"
        "{\n"
        f' "short_summary": "summary of the text in about {short_words} words, in {lang}",\n'
        f" {pts}"
        ' "keywords": ["6-10 important keywords from the text"],\n'
        ' "terms": [{"term": "difficult word from the text",'
        ' "meaning_hi": "simple meaning in Hindi (Devanagari)",'
        ' "meaning_en": "simple meaning in English"}]\n'
        "}\n"
        "'terms' must have 6-8 difficult words that appear in the text.\n\n"
        f"TEXT:\n{text}")
    r = ollama.chat(
        model=model, format="json", keep_alive=KEEP_ALIVE,
        messages=[{"role": "system", "content": "You output only valid JSON."},
                  {"role": "user", "content": prompt}],
        options=_opts(short_words + 450, "Hinglish", extra_ctx_tokens=len(text) // 2, temp=0.2),
    )
    d = _extract_json(r["message"]["content"])
    return {
        "short_summary": d.get("short_summary", ""),
        "key_points": d.get("key_points", []),
        "keywords": d.get("keywords", []),
        "terms": [t for t in d.get("terms", []) if isinstance(t, dict)],
    }


# ---------------------------------------------------------------- files
def prepare_source(text: str, model: str) -> str:
    """Large files: condense chunk-by-chunk into notes so they fit the context."""
    limit = CHUNK_CHARS * 2
    if len(text) <= limit:
        return text
    chunks = [text[i:i + CHUNK_CHARS] for i in range(0, len(text), CHUNK_CHARS)][:10]
    notes = []
    for c in chunks:
        r = ollama.chat(
            model=model, keep_alive=KEEP_ALIVE,
            messages=[{"role": "user", "content":
                       f"Write short notes (key facts, names, numbers) of this part:\n\n{c}"}],
            options={"temperature": 0.2, "num_predict": 300, "num_ctx": 4096})
        notes.append(r["message"]["content"].strip())
    return "\n".join(notes)[:limit]
