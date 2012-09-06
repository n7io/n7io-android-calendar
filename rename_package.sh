#!/bin/bash
#######################################################################################
# file EXCHANGES STRINGS
#
#
######################################################################################

OLDSTRING="com.android.calendar";
NEWSTRING="io.n7.calendar";

FILES=$(grep -r "${OLDSTRING}" ./ --exclude-dir=".svn" --exclude-dir=".git" | awk -F: '{print $1}' | grep src)

for FILE in ${FILES}
do
    echo "found file ${FILE} containing ${OLDSTRING}";
    #cp ${FILE}.BCK ${FILE};
    #cp ${FILE} ${FILE}.BCK;
    echo "sed 's:${OLDSTRING}:${NEWSTRING}:' <${FILE} >${FILE}.tmp";
    echo "mv ${FILE}.tmp ${FILE}";
done
