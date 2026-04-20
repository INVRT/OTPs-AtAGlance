import urllib.request
import os

font_dir = r"c:\Coding\otp-widget-ag\CabGlance2\app\src\main\res\font"
os.makedirs(font_dir, exist_ok=True)

urls = {
    "google_sans.ttf": "https://raw.githubusercontent.com/google/iosched-ios/master/Source/Resources/Fonts/ProductSans-Regular.ttf",
    "google_sans_bold.ttf": "https://raw.githubusercontent.com/google/iosched-ios/master/Source/Resources/Fonts/ProductSans-Bold.ttf"
}

for name, url in urls.items():
    print(f"Downloading {name}...")
    try:
        urllib.request.urlretrieve(url, os.path.join(font_dir, name))
        print("Done.")
    except Exception as e:
        print(f"Error: {e}")
