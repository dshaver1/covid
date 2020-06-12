EXPORT_DIR=/mnt/data/export
MANUAL_DATA_FILE=$EXPORT_DIR/covid.manualrawdata.dump
RAWDATAV1_FILE=$EXPORT_DIR/covid.rawdata.dump
RAWDATAV2_FILE=$EXPORT_DIR/covid.rawdatav2.dump
REPORTS_FILE=$EXPORT_DIR/covid.reports.dump

mongoexport --collection=manualrawdata --db="covid" --out=$MANUAL_DATA_FILE
mongoexport --collection=rawdata --db="covid" --out=$RAWDATAV1_FILE
mongoexport --collection=rawdatav2 --db="covid" --out=$RAWDATAV2_FILE
mongoexport --collection=reports --db="covid" --out=$REPORTS_FILE
zip $EXPORT_DIR/covid-backup-$(date +'%Y-%m-%dT%H-%M-%S').zip $MANUAL_DATA_FILE $RAWDATAV1_FILE $RAWDATAV2_FILE $REPORTS_FILE
rm $MANUAL_DATA_FILE
rm $RAWDATAV1_FILE
rm $RAWDATAV2_FILE
rm $REPORTS_FILE
