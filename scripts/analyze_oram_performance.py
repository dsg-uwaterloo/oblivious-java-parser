import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
import argparse
import os

def create_time_transform(total_duration, stretch_factor=0.5):
    """Create time transform with adjustable stretching
    stretch_factor: 0.1 (max stretch) to 1.0 (no stretch)"""
    
    def transform(x):
        return np.sign(x) * (np.abs(x) ** stretch_factor)
    
    def inverse_transform(x):
        return np.sign(x) * (np.abs(x) ** (1/stretch_factor))
    
    return transform, inverse_transform

def main():
    parser = argparse.ArgumentParser(description='Analyze ORAM performance data')
    parser.add_argument('file_path', help='Path to CSV data file')
    parser.add_argument('--linear-time', action='store_true',
                      help='Use linear time scale instead of transformed scale')
    args = parser.parse_args()

    if not os.path.exists(args.file_path):
        print(f"Error: File not found - {args.file_path}")
        sys.exit(1)

    try:
        # Data loading and processing
        dtype_spec = {
            'timestamp': 'int64', 'operation': 'category', 'duration_ns': 'int64',
            'blockId': 'string', 'isWrite': 'boolean', 'treeHeight': 'Int64',
            'stashSize': 'Int64', 'resized': 'boolean', 'leafPos': 'Int64',
            'oldTreeHeight': 'Int64', 'newTreeHeight': 'Int64'
        }

        df = pd.read_csv(
            args.file_path,
            dtype=dtype_spec,
            true_values=['true'],
            false_values=['false'],
            na_values=[''],
            keep_default_na=False
        )

        access_df = df[df['operation'] == 'access'].copy()
        if access_df.empty:
            print("No access operations found in the data")
            sys.exit(0)

        # Convert timestamps
        access_df['timestamp'] = pd.to_datetime(access_df['timestamp'], unit='ms')
        start_time = access_df['timestamp'].min()
        
        # Time transformation handling
        if args.linear_time:
            x_values = access_df['timestamp']
            time_label = 'Timestamp'
        else:
            access_df['rel_time'] = (access_df['timestamp'] - start_time).dt.total_seconds()
            total_duration = access_df['rel_time'].max()
            transform, _ = create_time_transform(total_duration, stretch_factor=0.3)
            x_values = access_df['rel_time'].apply(transform)
            time_label = 'Time Progression (% of total duration)'

        # Get resize events after potential rel_time calculation
        resize_events = access_df[access_df['resized']]

        # Visualization setup
        fig, axs = plt.subplots(3, 1, figsize=(14, 15))
        plot_args = {'linestyle': '-', 'linewidth': 2, 'alpha': 0.8}

        # Plot data
        axs[0].plot(x_values, access_df['duration_ns']/1e6, color='tab:blue', **plot_args)
        axs[0].set_ylabel('Duration (ms)', color='tab:blue')
        axs[0].tick_params(axis='y', labelcolor='tab:blue')

        axs[1].plot(x_values, access_df['stashSize'], color='tab:green', **plot_args)
        axs[1].set_ylabel('Stash Size')

        axs[2].step(x_values, access_df['treeHeight'], color='tab:red', where='post', **plot_args)
        axs[2].set_ylabel('Tree Height')

        # Axis formatting
        if args.linear_time:
            date_fmt = mdates.DateFormatter('%H:%M:%S')
            for ax in axs:
                ax.xaxis.set_major_formatter(date_fmt)
                ax.xaxis.set_major_locator(mdates.AutoDateLocator())
                plt.xticks(rotation=45)
        else:
            percentage_ticks = [0.1, 0.5, 1, 5, 10, 25, 50, 75, 95, 99, 100]
            tick_positions = transform((np.array(percentage_ticks)/100) * total_duration)
            for ax in axs:
                ax.set_xticks(tick_positions)
                ax.set_xticklabels([f"{p}%" for p in percentage_ticks])

        # Highlight resize events
        if not resize_events.empty:
            event_x = resize_events['timestamp'] if args.linear_time else transform(resize_events['rel_time'])
            for ax in axs:
                for x in event_x:
                    ax.axvline(x=x, color='red', linestyle='--', alpha=0.4, linewidth=1)

        axs[0].set_title(f'ORAM Performance Analysis - {"Linear" if args.linear_time else "Transformed"} Time Scale')
        plt.tight_layout()

        output_path = os.path.splitext(args.file_path)[0] + f'_{"linear" if args.linear_time else "transformed"}.png'
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        plt.close()
        
        print(f"Analysis saved to: {output_path}")

    except Exception as e:
        print(f"Error processing file: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()