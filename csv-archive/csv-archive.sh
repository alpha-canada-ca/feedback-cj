#!/bin/s
ls
older_Than_x_Days=$(date --date="55 days ago" +%F)
mongoexport --uri='$mongo_db_uri' --collection=problem -q='{ "problemDate": { "$lte" : "'"$older_Than_x_Days"'"}}' --type=csv --out=text2.csv --fields=problemDate,problemDetails,problem,tags,url,language,institution,section,theme
ls
sed -i 'text2.csv' \
  -Ee 's/[][]//g' \
  -e "1s/problemDate/Date/g" \
  -e "1s/problemDetails/Comment/g" \
  -e "1s/problem/Problem/g" \
  -e "1s/tags/Tags/g" \
  -e "1s/url/URL/g" \
  -e "1s/language/Language/g" \
  -e "1s/institution/Institution/g" \
  -e "1s/section/Section/g" \
  -e "1s/theme/Theme/g" \
  -e "1s/institution/Institution/g" text2.csv