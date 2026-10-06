"""Background themes + CSS generator."""

THEMES = {
    "🌌 Night Sky": {
        "bg": "linear-gradient(180deg,#02030f 0%,#0b1033 55%,#1b1b4d 100%)",
        "text": "#e8ecff", "card": "rgba(20,24,70,0.65)", "accent": "#7c8cff",
        "fx": "stars",
    },
    "✨ Night Starry": {
        "bg": "linear-gradient(180deg,#0a0420 0%,#241046 50%,#4a1d6b 100%)",
        "text": "#fdf1ff", "card": "rgba(60,25,100,0.6)", "accent": "#ffd86b",
        "fx": "stars_big",
    },
    "⛅ Cloudy Morning": {
        "bg": "linear-gradient(180deg,#cfd9e6 0%,#e9eef5 60%,#f7f3e8 100%)",
        "text": "#1f2a3a", "card": "rgba(255,255,255,0.7)", "accent": "#4a7bb7",
        "fx": "clouds",
    },
    "🌧️ Rainy": {
        "bg": "linear-gradient(180deg,#232b36 0%,#37444f 60%,#4b5b68 100%)",
        "text": "#e6eef5", "card": "rgba(30,40,50,0.65)", "accent": "#5ec8ff",
        "fx": "rain",
    },
    "🌅 Sunrise": {
        "bg": "linear-gradient(180deg,#ff9a8b 0%,#ffcf91 55%,#fff1c9 100%)",
        "text": "#3a2218", "card": "rgba(255,255,255,0.6)", "accent": "#e2553a",
        "fx": "",
    },
}

FX_CSS = {
    "stars": """
    .stApp::before{content:"";position:fixed;inset:0;pointer-events:none;z-index:0;
      background-image:
        radial-gradient(1.5px 1.5px at 20px 30px,#fff,transparent),
        radial-gradient(1px 1px at 80px 120px,#fff,transparent),
        radial-gradient(1.5px 1.5px at 150px 60px,#cfd8ff,transparent),
        radial-gradient(1px 1px at 220px 160px,#fff,transparent),
        radial-gradient(2px 2px at 300px 90px,#fff,transparent);
      background-size:320px 200px;animation:twinkle 4s ease-in-out infinite alternate;}
    @keyframes twinkle{from{opacity:.45}to{opacity:1}}
    """,
    "stars_big": """
    .stApp::before{content:"";position:fixed;inset:0;pointer-events:none;z-index:0;
      background-image:
        radial-gradient(2.5px 2.5px at 40px 50px,#ffe9a8,transparent),
        radial-gradient(2px 2px at 140px 140px,#fff,transparent),
        radial-gradient(3px 3px at 230px 40px,#ffd86b,transparent),
        radial-gradient(2px 2px at 310px 170px,#ffc4f4,transparent);
      background-size:360px 220px;animation:twinkle 3s ease-in-out infinite alternate;}
    @keyframes twinkle{from{opacity:.4}to{opacity:1}}
    """,
    "clouds": """
    .stApp::before{content:"";position:fixed;inset:0;pointer-events:none;z-index:0;
      background:
        radial-gradient(ellipse 220px 70px at 20% 15%,rgba(255,255,255,.85),transparent 70%),
        radial-gradient(ellipse 300px 90px at 70% 25%,rgba(255,255,255,.75),transparent 70%),
        radial-gradient(ellipse 260px 80px at 45% 8%,rgba(255,255,255,.7),transparent 70%);
      background-size:200% 100%;animation:drift 60s linear infinite;}
    @keyframes drift{from{background-position:0 0}to{background-position:-200% 0}}
    """,
    "rain": """
    .stApp::before{content:"";position:fixed;inset:0;pointer-events:none;z-index:0;
      background-image:linear-gradient(105deg,transparent 48%,rgba(160,210,255,.45) 50%,transparent 52%);
      background-size:26px 60px;animation:rain .6s linear infinite;opacity:.5;}
    @keyframes rain{from{background-position:0 0}to{background-position:-10px 60px}}
    """,
    "": "",
}


def build_css(name: str) -> str:
    t = THEMES.get(name, THEMES["🌌 Night Sky"])
    return f"""
    <style>
    .stApp{{background:{t['bg']};background-attachment:fixed;color:{t['text']};}}
    .stApp, .stApp p, .stApp li, .stApp label, .stApp span, .stApp h1, .stApp h2,
    .stApp h3, .stApp h4, .stApp div[data-testid="stMarkdownContainer"]{{color:{t['text']} !important;}}
    section[data-testid="stSidebar"]{{background:{t['card']};backdrop-filter:blur(8px);}}
    .block-container{{position:relative;z-index:1;}}
    .card{{background:{t['card']};border:1px solid {t['accent']}55;border-radius:16px;
           padding:1rem 1.2rem;margin:.6rem 0;backdrop-filter:blur(6px);}}
    .term{{border-left:4px solid {t['accent']};padding:.3rem .8rem;margin:.4rem 0;
           background:{t['card']};border-radius:8px;}}
    .stButton>button{{border:1px solid {t['accent']};border-radius:10px;}}
    {FX_CSS[t['fx']]}
    </style>
    """
