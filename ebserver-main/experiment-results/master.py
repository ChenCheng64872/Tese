import os
import glob
import pandas as pd

# --- CONFIGURATION ---
ROOT_DIR = "." 
OUTPUT_FILE = "master_energy_data.csv"

def extract_metadata_from_path(filepath):
    path_parts = os.path.normpath(os.path.dirname(filepath)).split(os.sep)
    meta = {}
    
    known_apps = {'IG', 'WPP', 'LIGHT', 'YT', 'TT'}
    for part in path_parts:
        if part.upper() in known_apps:
            meta['App'] = part.upper()

    for part in path_parts:
        if '-' in part:
            try:
                key, value = part.split('-', 1)
                if key not in ['batterystats', '2026']: 
                    col_name = f"Config_{key.capitalize()}"
                    meta[col_name] = value
            except ValueError:
                continue 

    meta['Experiment_ID'] = os.path.dirname(filepath)
    
    return meta

def load_and_aggregate(root_path):
    all_files = glob.glob(os.path.join(root_path, "**", "results_complete.csv"), recursive=True)
    
    if not all_files:
        print("❌ No 'results_complete.csv' files found. Did you run the previous script?")
        return None

    df_list = []
    
    for f in all_files:
        try:
            df = pd.read_csv(f)
            
            meta = extract_metadata_from_path(f)
            
            for key, val in meta.items():
                df[key] = val
            
            df_list.append(df)
        except Exception as e:
            print(f"⚠️ Error reading {f}: {e}")

    if df_list:
        master_df = pd.concat(df_list, ignore_index=True)
        return master_df
    return None

def generate_summary(df):
    """Prints useful statistics."""
    
    print("\n" + "="*40)
    print("📊 DATA DATASET SUMMARY")
    print("="*40)

    # 1. Total Duration
    total_ms = df['duration_ms'].sum()
    total_hours = total_ms / (1000 * 60 * 60)
    print(f"⏱️  Total Experiment Time Logged: {total_hours:.2f} hours")
    print(f"📝 Total Data Points (Rows): {len(df)}")
    
    return total_ms

if __name__ == "__main__":
    df = load_and_aggregate(ROOT_DIR)
    
    if df is not None:
        df.to_csv(OUTPUT_FILE, index=False)
        print(f"\n💾 Master DataFrame saved to: {OUTPUT_FILE}")
        
        generate_summary(df)