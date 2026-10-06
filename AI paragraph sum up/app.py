"""AI Paragraph Generator & File Summarizer — Streamlit + Llama 3.2 (Ollama).
Run:  streamlit run app.py   (or just press the Run button)
"""
import sys
from streamlit.runtime import exists

if not exists():  # allows the normal ▶ Run button in VS Code
    import subprocess
    subprocess.run([sys.executable, "-m", "streamlit", "run", __file__])
    sys.exit()

import html
import time

import streamlit as st

import history
import llm
from file_reader import read_file
from themes import THEMES, build_css

st.set_page_config(page_title="AI Paragraph Generator", page_icon="🧠", layout="wide")

# ---------------- Sidebar ----------------
with st.sidebar:
    st.header("👤 Profile")
    user = st.text_input("Your name", value=st.session_state.get("user", ""),
                         placeholder="Enter your name").strip()
    st.session_state["user"] = user

    st.header("🎨 Theme")
    theme = st.selectbox("Background", list(THEMES.keys()), label_visibility="collapsed")

    st.header("⚙️ Settings")
    model = st.text_input("Ollama model", value=llm.DEFAULT_MODEL)
    lang = st.selectbox("Output language", ["English", "Hindi", "Hinglish"])

    st.header("🕘 History")
    key = user or "guest"
    items = history.get(key)
    if not items:
        st.caption("No searches yet.")
    for i, it in enumerate(items):
        icon = "📄" if it["kind"] == "file" else "🔎"
        if st.button(f"{icon} {it['title'][:28]}  · {it['time']}", key=f"h{i}"):
            st.session_state["result"] = it["result"]
            st.session_state["result_title"] = it["title"]
    if items and st.button("🗑️ Clear history"):
        history.clear(key)
        st.rerun()

st.markdown(build_css(theme), unsafe_allow_html=True)

# ---------------- Main ----------------
st.title(f"🧠 AI Paragraph Generator{f' — Hi, {user}!' if user else ''}")
st.caption("Paragraph → Short summary → Keywords → Hard words (Hindi + English meaning)")

c_a, c_b = st.columns([1, 2])
words = c_a.number_input("📏 Paragraph length (words)", min_value=50, max_value=3000,
                         value=300, step=50,
                         help="Any number, e.g. 100, 500, 3000. Bigger = takes longer.")
exact = c_b.checkbox("🎯 Exact word count (slower — one extra pass)", value=False)

tab_topic, tab_file = st.tabs(["🔎 Topic", "📂 Upload File"])


def generate(source: str, title: str, kind: str, is_topic: bool):
    t0 = time.time()
    try:
        if not is_topic:
            with st.spinner("File ke notes ban rahe hain…"):
                source = llm.prepare_source(source, model)
        st.markdown(f"## 📌 {title}")
        st.caption("✍️ Likh raha hai… (text live dikh raha hai)")
        text = st.write_stream(llm.stream_paragraph(source, int(words), lang, model, is_topic))
        if exact:
            with st.spinner("Word count adjust ho raha hai…"):
                text = llm.fix_length(text, int(words), lang, model)
        with st.spinner("Short summary, keywords aur meanings ban rahe hain…"):
            info = llm.analyze(text, lang, model, llm.short_words_for(int(words)),
                               with_points=not is_topic)
        res = {"paragraph": text, "words_actual": llm.count_words(text),
               "elapsed": round(time.time() - t0, 1), **info}
        st.session_state["result"] = res
        st.session_state["result_title"] = title
        history.add(key, title, kind, int(words), res)
    except Exception as e:
        st.error(f"Error: {e}\n\nCheck that Ollama is running and the model is pulled: "
                 f"`ollama pull {model}`")
        return
    st.rerun()


with tab_topic:
    topic = st.text_input("Enter a topic", placeholder="e.g. Black holes, Machine Learning…")
    if st.button("Generate paragraph", type="primary", key="go_topic"):
        if topic.strip():
            generate(topic.strip(), topic.strip(), "topic", True)
        else:
            st.warning("Please enter a topic.")

with tab_file:
    up = st.file_uploader("Upload PDF / DOCX / TXT", type=["pdf", "docx", "txt", "md"])
    if st.button("Summarize file", type="primary", key="go_file"):
        if up is None:
            st.warning("Please upload a file first.")
        else:
            text = read_file(up)
            if len(text.strip()) < 50:
                st.error("Could not read enough text from this file (scanned PDF?).")
            else:
                generate(text, up.name, "file", False)

# ---------------- Result ----------------
res = st.session_state.get("result")
if res:
    title = st.session_state.get("result_title", "")
    para = res.get("paragraph") or res.get("summary", "")
    st.markdown(f"## 📌 {title}")
    meta = f"{res.get('words_actual', llm.count_words(para))} words"
    if res.get("elapsed"):
        meta += f" · ⏱️ {res['elapsed']} sec"
    st.markdown(f"<div class='card'><b>📝 Paragraph</b> <small>({meta})</small><br><br>"
                f"{html.escape(para).replace(chr(10), '<br>')}</div>", unsafe_allow_html=True)

    short = res.get("short_summary", "")
    if short:
        st.markdown(f"<div class='card'><b>⚡ Short Summary</b> "
                    f"<small>({llm.count_words(short)} words)</small><br><br>"
                    f"{html.escape(short)}</div>", unsafe_allow_html=True)

    if res.get("key_points"):
        st.markdown("### ⭐ Key Points")
        for p in res["key_points"]:
            st.markdown(f"- {p}")

    if res.get("keywords"):
        st.markdown("### 🏷️ Keywords")
        st.markdown(" ".join(f"`{k}`" for k in res["keywords"]))

    if res.get("terms"):
        st.markdown("### 📖 Hard Words — Hindi & English meaning")
        for t in res["terms"]:
            hi = html.escape(str(t.get("meaning_hi") or t.get("meaning", "")))
            en = html.escape(str(t.get("meaning_en", "")))
            st.markdown(f"<div class='term'><b>{html.escape(str(t.get('term','')))}</b><br>"
                        f"🇮🇳 {hi}<br>🇬🇧 {en}</div>", unsafe_allow_html=True)

    export = (f"# {title}\n\n## Paragraph\n{para}\n\n## Short Summary\n{short}\n\n"
              "## Keywords\n" + ", ".join(res.get("keywords", [])) +
              "\n\n## Hard Words\n" +
              "\n".join(f"- {t.get('term')}: {t.get('meaning_hi','')} | {t.get('meaning_en','')}"
                        for t in res.get("terms", [])))
    st.download_button("⬇️ Download as .md", export, file_name="summary.md")
