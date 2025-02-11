# Modular Vehicle Routing Problem: Applications in Logistics

## Introduction

This repository contains the source code and data for the following paper:

**Zhou, H., Li, Y., Ma, C., Long, K., & Li, X. (2025).** *Modular Vehicle Routing Problem: Applications in Logistics.* Transportation Research Part E: Logistics and Transportation Review.

[Modular Vehicle Routing Problem: Applications in Logistics](https://arxiv.org/abs/2409.01518)

Modular vehicles (MVs) can dock and split dynamically during trips, offering new possibilities for logistics optimization. This repository provides implementations for the **Modular Vehicle Routing Problem with Time Window (MVRPTW)**, where MVs can operate independently or form platoons to reduce delivery costs. The repository includes the benchmark instances used in our paper and the mixed-integer linear programming models solvable with commercial solvers, as well as a Tabu Search algorithm designed for large-scale instances.

## Usage

### Code

The project is developed using Java 1.8.0. Some parts of the code require the CPLEX solver. The visualization components use Python 3. Please ensure you have the corresponding environment set up.

The code related to our data processing and algorithm includes:

- **analysis** - Code used to generate figures.
- **algorithm** - Code for the TS algorithm and MILP model for the MVRPTW. To run the algorithm, use "src/Main/Run". To run the MILP models, use "src/MIL/Run2".

As you proceed through all code, always verify the paths for both the input and output files. This ensures that everything runs smoothly.

### Data

The '**data**' folder contains the benchmark instances used in our paper.

## Developers

Developers - Hang Zhou (hzhou364@wisc.edu).

If you have any questions, please feel free to contact the CATS Lab at UW-Madison. We're here to help!