#!/bin/s
older_Than_x_Days=$(date --date="83 days ago" +%F)
mongoexport --uri="mongodb uri link" --collection=problem -q='{ "problemDate": { "$lte" : "'"$older_Than_x_Days"'"}}' --type=csv --out=text2.csv --fields=problemDate,problemDetails,problem,tags,url,language,institution,section,theme
sed -i '' \
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