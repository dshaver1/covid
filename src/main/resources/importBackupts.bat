H:
cd H:\dev\workingdir\covid\local-backups\2020-06-12

mongoimport --collection=manualrawdata --db="covid" --file="covid.manualrawdata.dump"
mongoimport --collection=rawdata --db="covid" --file="covid.rawdata.dump"
mongoimport --collection=rawdatav2 --db="covid" --file="covid.rawdatav2.dump"
mongoimport --collection=reports --db="covid" --file="covid.reports.dump"