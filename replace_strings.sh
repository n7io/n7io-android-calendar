FILES=$(grep -r "android.provider.Calendar" ./ --exclude-dir=".svn" --exclude-dir=".git" | awk -F: '{print $1}' | grep -v 'BCK' | grep "caldav")

for FILE in ${FILES}
do
   #cp ${FILE} ${FILE}.BCK;
   sed 's:android.provider.Calendar:android.provider.CalendarContract:' < ${FILE} > ${FILE}.tmp
   mv ${FILE}.tmp ${FILE} 
done
