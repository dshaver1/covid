H:
cd H:\dev\workingdir\covid\local-backups\2020-06-12

mongoexport --collection=manualrawdata --db="covid" --out="covid.manualrawdata.local.dump"
mongoexport --collection=rawdata --db="covid" --out="covid.rawdata.local.dump"
mongoexport --collection=rawdatav2 --db="covid" --out="covid.rawdatav2.local.dump"
mongoexport --collection=reports --db="covid" --out="covid.reports.local.dump"