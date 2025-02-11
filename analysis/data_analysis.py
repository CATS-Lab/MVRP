import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

# Set global font to Times New Roman with size 25
plt.rcParams['font.family'] = 'Times New Roman'
plt.rcParams['font.size'] = 25

def sensitivity():
    # Read data from CSV file
    df = pd.read_csv('data.csv')

    # Calculate percentage ratios relative to L=1
    df['Ratio_L3'] = df['L=3'] / df['L=1'] * 100  # L=1 is set as 100%
    df['Ratio_L2'] = df['L=2'] / df['L=1'] * 100  # L=1 is set as 100%

    # Set figure size
    plt.figure(figsize=(10, 6))

    # Plot percentage ratios for each row
    for index, row in df.iterrows():
        plt.plot(['L=1', 'L=2', 'L=3'], [100, row['Ratio_L2'], row['Ratio_L3']], marker='o', label=row['Instance'])

    # Set axis labels
    plt.ylabel('Percentage (%)')
    plt.xticks(ticks=[0, 1, 2], labels=['1', '2', '3'])

    # Adjust layout to prevent overlapping
    plt.tight_layout()

    # Save the figure as a JPG file
    plt.savefig('percentage_ratios1.jpg', format='jpg', dpi=300)

def sensitivity2():
    # Read data from CSV file
    df = pd.read_csv('data.csv')

    # Calculate percentage ratios relative to L=1
    df['Ratio_L3'] = df['eta=0.2'] / df['eta=0.05'] * 100  # L=1 is set as 100%
    df['Ratio_L2'] = df['eta=0.1'] / df['eta=0.05'] * 100  # L=1 is set as 100%

    # Set figure size
    plt.figure(figsize=(10, 6))

    # Plot percentage ratios for each row
    for index, row in df.iterrows():
        plt.plot(['L=1', 'L=2', 'L=3'], [100, row['Ratio_L2'], row['Ratio_L3']], marker='o', label=row['Instance'])

    # Set axis labels
    plt.ylabel('Percentage (%)')
    plt.xticks(ticks=[0, 1, 2], labels=['0.05', '0.1', '0.02'])

    # Adjust layout to prevent overlapping
    plt.tight_layout()

    # Save the figure as a JPG file
    plt.savefig('percentage_ratios2.jpg', format='jpg', dpi=300)

def compare_algorithm_components():
    # Read data from CSV file
    df = pd.read_csv('compare.csv')

    # Set figure size
    plt.figure(figsize=(12, 6))

    # Extract algorithm names
    instances = df['Algorithm']

    # Calculate percentages relative to 'None'
    values_1 = df['None']
    values_2 = (df['Relocate'] / values_1) * 100
    values_3 = (df['Merge'] / values_1) * 100
    values_4 = (df['Shaking'] / values_1) * 100
    values_5 = (df['Multi-start'] / values_1) * 100

    # Define x-axis labels
    x_labels = ['none', 'relocation', 'merging', 'shaking', 'multi-start']

    # Plot percentage comparisons for each algorithm
    for index, instance in enumerate(instances):
        plt.plot(x_labels, [100, values_2[index], values_3[index], values_4[index], values_5[index]],
                 marker='o', label=instance)

    # Set y-axis label
    plt.ylabel('Percentage (%)')

    # Adjust layout to prevent overlapping
    plt.tight_layout()

    # Save the figure as a JPG file
    plt.savefig('comparison_percentage_line_chart.jpg', format='jpg', dpi=300, bbox_inches='tight')

def convergence():
    # Read data from CSV file
    df = pd.read_csv('converge.csv')

    # Extract iterations and objective values
    iterations = df['Iterations']
    objective_values = df['ObjectiveValue']

    # Plot convergence curve
    plt.figure(figsize=(12, 6))
    plt.plot(iterations, objective_values, label='Objective Value')
    plt.xlabel('Iterations')
    plt.ylabel('Optimal Objective Values')

    # Set x-axis ticks as integers with step size 50
    plt.xticks(np.arange(1, max(iterations) + 2, 50))

    # Adjust layout to prevent overlapping
    plt.tight_layout()

    # Save the figure as a PNG file
    plt.savefig('converge_opt.png')

def main():
    # Uncomment the desired function calls to generate plots
    sensitivity()
    sensitivity2()
    compare_algorithm_components()
    convergence()

if __name__ == '__main__':
    main()
