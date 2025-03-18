import random
import shlex
import subprocess
import sys
import time
from datetime import datetime
from io import TextIOWrapper
from typing import Generator, Tuple

import matplotlib.pyplot as plt
import pandas as pd


def generate_test_sequence_pairs(max_length: int) -> Generator[Tuple[str, str], None, None]:
    if max_length < 1:
        raise ValueError("max_length must be at least 1")

    dna_bases = ['A', 'T', 'C', 'G']

    for length in range(1, max_length + 1):
        seq1 = ''.join(random.choices(dna_bases, k=length))
        seq2 = ''.join(random.choices(dna_bases, k=length))
        yield seq1, seq2

def compile_java(class_name: str, output_file: TextIOWrapper, error_file: TextIOWrapper) -> None:
    """
    Search path : src/main/java/com/example/
    """

    _dir = "src/main/java/com/example"

    cmd = shlex.split(f"javac {_dir}/{class_name}.java {_dir}/PathORAM.java")
    proc = subprocess.run(cmd, capture_output=True, text=True)

    if proc.returncode != 0:
        error_file.write(proc.stderr)
        error_file.flush()
        # Raise an exception here, because we want to stop the program if there is an error compiling
        raise Exception(f"Error compiling {class_name}, see {error_file.name} for details")

    output_file.write(proc.stdout)
    output_file.flush()

DurationSecs = float
def run_java(class_name: str, args: Tuple, output_file: TextIOWrapper, error_file: TextIOWrapper) -> DurationSecs:
    arg_str = " ".join(args)
    cmd = shlex.split(f"java -cp src/main/java com.example.{class_name} {arg_str}")

    start = time.perf_counter()
    proc = subprocess.run(cmd, capture_output=True, text=True)
    end = time.perf_counter()

    if proc.returncode != 0:
        error_file.write(proc.stderr)
        error_file.flush()
        # Do not raise an exception here, because we want to continue running the other programs
        print(f"[ERROR] Error running {class_name}, see {error_file.name} for details")

    output_file.write(proc.stdout)
    output_file.flush()

    return end - start

def plot_seq_len_vs_duration(metrics_file: TextIOWrapper, image_file: TextIOWrapper) -> None:
    df = pd.read_csv(metrics_file)

    # Plot line for each program
    for program in df["program"].unique():
        df_program = df[df["program"] == program]
        plt.plot(df_program["seq_len"], df_program["duration"], label=program)

    plt.xlabel("Sequence Length")
    plt.ylabel("Duration (seconds)")
    plt.title("Sequence Length vs Duration")
    plt.legend()
    plt.savefig(image_file)

COMPARE = ["SmithWaterman", "SmithWatermanTransformed"]
def main():
    if len(sys.argv) != 2:
        print(f"[ERROR] Usage: python {sys.argv[0]} <max_length>")
        sys.exit(1)

    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")

    metrics_file_path = f"smith_waterman_metrics_{timestamp}.csv"

    with open(f"smith_waterman_output_{timestamp}.txt", "w") as output_file, \
        open(f"smith_waterman_error_{timestamp}.txt", "w") as error_file, \
        open(metrics_file_path, "w") as metrics_file:
        for class_name in COMPARE:
            compile_java(class_name, output_file, error_file)

        metrics_file.write("program,seq_len,duration\n")

        for seq1, seq2 in generate_test_sequence_pairs(int(sys.argv[1])):
            for program in COMPARE:
                print(f"[INFO] Running {program} on {seq1} and {seq2}")
                duration = run_java(program, (seq1, seq2), output_file, error_file)
                metrics_file.write(f"{program},{len(seq1)},{duration}\n")

    plot_seq_len_vs_duration(metrics_file_path, f"smith_waterman_plot_{timestamp}.png")



if __name__ == "__main__":
    main()