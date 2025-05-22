# Atheroma Analyzer Plugin for ImageJ

**Atheroma Analyzer** is a plugin for ImageJ designed to facilitate the quantitative analysis of atheroma plaques in histological or medical images. It provides a graphical interface for selecting, processing, and analyzing regions of interest (ROIs), focusing on the segmentation and quantification of the aorta and plaques.

## Main Features

- Load and preview multiple images from a directory.
- Select color channels and background types for analysis.
- Automatic ROI detection using color subtraction and thresholding.
- Manual ROI editing tools (Freehand, Spline).
- Morphological operations (erosion and dilation).
- Measurement of areas and relative intensities of aorta and plaque.
- Export results in CSV format.
- Save processed images as TIFF files.

## Requirements

- Java 8 or higher.
- ImageJ (preferably the Fiji distribution).
- INRA IJPB Morphology Library for morphological operations.

## Installation

1. Compile or download the JAR file of the plugin.
2. Place it in the `plugins` folder of ImageJ/Fiji.
3. Restart ImageJ.
4. Access the plugin via `Plugins > Atheroma Analyzer`.

## Workflow

1. Select a directory of images.
2. Define a region of interest (ROI) with the rectangle tool.
3. Process the image to segment the aorta and plaque.
4. View and adjust the ROIs if necessary.
5. Export the results and save the processed images.

## Outputs

- CSV file with quantitative data per image.
- TIFF images of the masks and generated montages.
- Individual and total results tables.

## Application

This plugin is useful for researchers in atherosclerosis and vascular pathology, enabling semi-automated quantification of plaque burden in histological images.

## License

Distributed under the MIT License.

