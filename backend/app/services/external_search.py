from urllib.parse import quote

PROVIDER = "10000RECIPE"
BASE_URL = "https://www.10000recipe.com/recipe/list.html"


def build_url(title: str) -> str:
    return f"{BASE_URL}?q={quote(title)}"
