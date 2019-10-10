Oskari.registerLocalization(
{
    "lang": "en",
    "key": "MyPlacesImport",
    "value": {
        "title": "Own datasets",
        "desc": "",
        "tool": {
            "tooltip": "Import your own datasets."
        },
        "flyout": {
            "title": "Dataset Import",
            "description": "Upload a dataset from your computer as a zipped file which contains all required files from one of the following file formats: <ul><li>Shapefile (.shp, .shx, .dbf and .prj, optionally .cpg)</li><li>GPX-file (.gpx)</li><li>MapInfo (.mif and .mid)</li><li>Google Map (.kml)</li></ul>The zipped file can contain only one dataset and it can be at most {maxSize, number} Mb.",
            "help": "Upload a dataset from your computer as a zipped file. Please check that all the files are in the correct file format and coordinate reference system.",
            "actions": {
                "cancel": "Cancel",
                "next": "Next",
                "close": "Close",
                "submit": "Import"
            },
            "layer": {
                "title": "Dataset Information",
                "name": "Map layer name",
                "desc": "Description",
                "source": "Data source",
                "style": "Style definitions",
                "srs": "EPSG code"
            },
            "validations": {
                "error": {
                    "title": "Error",
                    "message": "The dataset has not been imported. A file and a name are missing. Please correct them and try again."
                }
            },
            "finish": {
                "success": {
                    "title": "Dataset Import Succeeded",
                    "message": "The dataset has been imported with {count, plural, one {# feature} other {# features}}. You can now find it in the \"My data\" menu."
                },
                "failure": {
                    "title": "The dataset could not be imported."
                }
            },
            "error":{
                "title": "The dataset could not be imported.",
                "timeout": "The dataset import couldn't be finished because of a time-out error.",
                "abort": "The dataset import were aborted.",
                "generic": "The dataset import failed.",
                "hasFolders": "NB! Check that the files of the input data are not stored inside a folder within the zip file.",

                // unknown_projection
                "noSrs": "Unable to identify the coordinate system from the file. Check that the coordinate system is correctly specified in the data or enter the ESPG code in number format (e.g. 4326) and try again.",
                "shpNoSrs": "The coordinate system could not be identified based on the shapefile. Include the .prj file that specifies the coordinate system in the compressed folder or enter the ESPG code in number format (e.g. 4326) and try again.",

                // parser_error
                "parserGeneric": "Couldn't process dataset.",
                "kml":"Couldn't create dataset from the KML file.",
                "shp":"Couldn't create dataset from the Shapefile.",
                "mif":"Couldn't create dataset from the MIF file.",
                "gpx":"Couldn't create dataset from the GPX file.",

                // Error codes from backend
                "no_main_file": "Couldn't find valid import file in the zip file. Please check that the file format is supported and it's a zipped file.",
                "too_many_files": "The zip file contained redundant files. Remove the redundant files and keep only those that are required according to the instructions.",
                "multiple_extensions":"Multiple files with the same {extension} file extension were found from the input data. The input data can only contain data sets of one file.",
                "multiple_main_file": "Multiple different data sets ({extensions}) were found from the input data. The input data can only contain data of one file.",
                "unable_to_store_data":"Unable to save the features of the input data. Check that all files required by the file format are inside a zip file and that the features of the input data are valid.",
                // "short_file_prefix":"Couldn't get the import file set - Prefix string too short",
                "file_over_size": "The selected file is too large. It can be at most {maxSize, number} Mb.",
                "no_features":"Couldn't find features in the input data. Check that the coordinates of the features are defined.",
                // "malformed":"Please check that the file names are in the correct format (no Scandinavian alphabets).",
                "invalid_epsg": "The entered ESPG code was not identified. Check that it is correct and in number format (e.g. 4326). If the code can't be identified despite this, the coordinate system information needs to be added to the data.",
                "format_failure": "The imported file is not valid. Verify the validity of the data and try again."
            },
            "warning":{
                "features_skipped":"Caution! During import {count, plural, one {# feature} other {# features}} where rejected because of missing or corrupted coordinates or geometry"
            }
        },
        "tab": {
            "title": "Datasets",
            "editLayer": "Edit map layer",
            "deleteLayer": "Delete map layer",
            "grid": {
                "name": "Name",
                "description": "Description",
                "source": "Data source",
                "edit": "Edit",
                "editButton": "Edit",
                "remove": "Delete",
                "removeButton": "Delete"
            },
            "confirmDeleteMsg": "Do you want to delete the dataset \"{name}\"?",
            "buttons": {
                "ok": "OK",
                "save": "Save",
                "cancel": "Cancel",
                "delete": "Delete",
                "close": "Close"
            },
            "notification": {
                "deletedTitle": "Dataset Delete",
                "deletedMsg": "The dataset has been deleted.",
                "editedMsg": "The dataset has been updated."
            },
            "error": {
                "title": "Error",
                "generic": "A system error occurred.",
                "deleteMsg": "Deleting the dataset failed due to an error in the system. Please try again later.",
                "editMsg": "Updating the dataset failed due to an error in the system. Please try again later.",
                "getStyle": "The style defined for the dataset was not found. Default values are shown on the form. Change the style definitions before saving the changes.",
                "styleName": "Give the map layer a name and try again."
            }
        },
        "layer": {
            "organization": "Own datasets",
            "inspire": "Own datasets"
        }
    }
});
