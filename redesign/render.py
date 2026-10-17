import subprocess, sys, os
here = os.path.dirname(os.path.abspath(__file__))
chrome = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
for spec in sys.argv[1:]:
    name, w, h = spec.split(":")
    out = os.path.join(here, name + ".png")
    url = "file:///" + os.path.join(here, "src", name + ".html").replace("\\", "/")
    subprocess.run([chrome, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--allow-file-access-from-files",
                    "--force-device-scale-factor=2", f"--window-size={w},{h}", "--virtual-time-budget=3000",
                    f"--screenshot={out}", url], capture_output=True)
    print(out, os.path.exists(out))
