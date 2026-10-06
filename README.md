# 🧠 AI Paragraph Generator & File Summarizer

A Python app powered by **Llama 3.2** (running locally through **Ollama**) with a **Streamlit** interface. Enter a topic and a word count to get a paragraph of that length, followed by a short summary, keywords, and difficult words with meanings in Hindi and English. You can also upload a document and get a summary with the key points.

## ✨ Features

- **Topic to paragraph:** choose any length from 50 to 3000 words. The text appears live while it is being written.
- **Short summary:** a condensed version of the generated paragraph. Its length scales with the paragraph (about 30 to 200 words).
- **Keywords:** the most important terms from the text.
- **Hard words with meanings:** difficult words listed with simple meanings in both **Hindi** and **English**.
- **File upload:** upload a **PDF, DOCX, TXT or MD** file to get a summary, key points, keywords, and word meanings. Large files are processed in chunks.
- **Exact word count option:** an optional extra pass to match the requested length more closely (slower).
- **Output languages:** English, Hindi, or Hinglish.
- **Themes:** Night Sky, Night Starry, Cloudy Morning, Rainy, and Sunrise, with animated backgrounds.
- **User profile and history:** enter your name in the sidebar. Your past searches are saved per user and can be reopened with one click.
- **Download:** export any result as a Markdown (`.md`) file.

## 🛠️ Tech Stack

- Python
- Streamlit
- Ollama with Llama 3.2
- pypdf and python-docx for reading files

## 📁 Project Structure

```
ai_summarizer/
├── app.py            # Streamlit UI
├── llm.py            # Llama 3.2 logic: paragraph, summary, keywords, meanings
├── file_reader.py    # Text extraction from PDF / DOCX / TXT
├── themes.py         # Background themes and CSS
├── history.py        # Per-user search history (stored locally)
├── requirements.txt
└── README.md
```

## 🚀 Getting Started

1. **Install Ollama** from [ollama.com](https://ollama.com) and download the model:
```bash
   ollama pull llama3.2
```
2. **Clone this repository** and install the dependencies:
```bash
   git clone https://github.com/<your-username>/<your-repo>.git
   cd <your-repo>
   python -m pip install -r requirements.txt
```
3. **Run the app:**
```bash
   python -m streamlit run app.py
```
4. Open `http://localhost:8501` in your browser if it does not open automatically.

## ⚠️ Notes

- The app needs **Ollama running locally**, so it cannot run on Streamlit Community Cloud as it is. To host it online, the model calls would need to be switched to a hosted API.
- Generation speed depends on your hardware. Short paragraphs are quick, while very long ones (1000+ words) can take longer, especially without a GPU.
- Scanned PDFs (images only) are not supported because no text can be extracted from them.
- Hindi output quality is lower than English with the small Llama 3.2 model.

## 📌 Future Ideas

- Hosted API support for online deployment
- PDF export of results
- Voice input
- More themes
