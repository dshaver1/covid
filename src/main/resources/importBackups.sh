EXPORT_DIR=/mnt/data/export
MANUAL_DATA_FILE=$EXPORT_DIR/covid.manualrawdata.dump
RAWDATAV1_FILE=$EXPORT_DIR/covid.rawdata.dump
RAWDATAV2_FILE=$EXPORT_DIR/covid.rawdatav2.dump
REPORTS_FILE=$EXPORT_DIR/covid.reports.dump

unzip $1 $EXPORT_DIR

mongoimport --collection=manualrawdata --db="covid" --file=$MANUAL_DATA_FILE
mongoimport --collection=rawdata --db="covid" --file=$RAWDATAV1_FILE
mongoimport --collection=rawdatav2 --db="covid" --file=$RAWDATAV2_FILE
mongoimport --collection=reports --db="covid" --file=$REPORTS_FILE

rm $MANUAL_DATA_FILE
rm $RAWDATAV1_FILE
rm $RAWDATAV2_FILE
rm $REPORTS_FILE
