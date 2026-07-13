import os
import json

base_dir = r"c:\Users\PCD\Desktop\malang_key\app\src\main\assets\ime\keyboard\org.florisboard.layouts\layouts"

mappings = {
    "1": ["&", "#", "%", "$", "\\"],
    "2": ["~", "-", "_"],
    "3": ["/", "@"],
    "4": ["\"", "'"],
    "5": ["☆", "★", "♡", "♥"],
    "6": ["^"],
    "7": ["(", "{", "[", "<"],
    "8": [")", "}", "]", ">"],
    "9": ["*"],
    "0": [";", ":"],
    "=": ["＋", "－", "±", "×", "÷"],
    "?": ["!"]
}

def get_code(char):
    if len(char) == 1:
        return ord(char)
    return 0

def update_dict(d, is_characters):
    changed = False
    
    if is_characters:
        # In characters layouts, we look for popup -> main -> label == "1"
        if "popup" in d and isinstance(d["popup"], dict):
            popup = d["popup"]
            if "main" in popup and isinstance(popup["main"], dict):
                label = popup["main"].get("label")
                if label in mappings:
                    popup["relevant"] = [{"code": get_code(c), "label": c} for c in mappings[label]]
                    changed = True
    else:
        # In numericRow or symbols, we look for label == "1"
        label = d.get("label")
        if label in mappings:
            if "popup" not in d or not isinstance(d["popup"], dict):
                d["popup"] = {}
            # Keep existing main if it exists, otherwise don't worry
            d["popup"]["relevant"] = [{"code": get_code(c), "label": c} for c in mappings[label]]
            changed = True
            
    for k, v in d.items():
        if isinstance(v, dict):
            if update_dict(v, is_characters):
                changed = True
        elif isinstance(v, list):
            for item in v:
                if isinstance(item, dict):
                    if update_dict(item, is_characters):
                        changed = True
    return changed

def process_dir(sub_dir, is_characters):
    path = os.path.join(base_dir, sub_dir)
    if not os.path.exists(path):
        return
    for root, dirs, files in os.walk(path):
        for file in files:
            if file.endswith(".json"):
                filepath = os.path.join(root, file)
                with open(filepath, 'r', encoding='utf-8') as f:
                    try:
                        data = json.load(f)
                    except:
                        continue
                
                changed = False
                if isinstance(data, list):
                    for item in data:
                        if isinstance(item, list):
                            for sub_item in item:
                                if isinstance(sub_item, dict):
                                    if update_dict(sub_item, is_characters):
                                        changed = True
                        elif isinstance(item, dict):
                            if update_dict(item, is_characters):
                                changed = True
                
                if changed:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        json.dump(data, f, ensure_ascii=False, indent=2)
                    print(f"Updated {filepath}")

process_dir("characters", True)
process_dir("numericRow", False)
process_dir("symbols", False)
process_dir("symbols2", False)
process_dir("symbolsMod", False)
process_dir("symbols2Mod", False)
