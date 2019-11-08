#!/bin/bash

update_folders=(
	lib
	webcontent
)

update_files=(
	rsdb.jar
)

GREEN='\033[0;32m'
RED='\033[0;31m'
WHITE='\033[0;97m'
NC='\033[0m' # No Color
echo -e ""
echo -e "${WHITE}--------------------------------------------------------------------------------------------------${NC}"
echo -e "${RED}!! only proceed if you stopped a running RSDB instance !!${NC}"
echo -e "${GREEN}Do you want to download the latest RSDB release package, backup changed files and performe update?${NC}"
echo -e "${WHITE}--------------------------------------------------------------------------------------------------${NC}"
select yn in "Yes" "No" "Cancel"; do
    case $yn in
        Yes ) break;;
        No ) exit;;
	Cancel ) exit;;
    esac
done

timestamp=$(date +%Y_%m_%d__%H_%M_%S)
echo $timestamp

echo -e "${GREEN}delete folder 'update'${NC}"
rm -rf ./update
if [ -d ./update ]
then
	echo -e "${RED}folder 'update' still exists, abort. (no changes performed)${NC}"
	exit 1
fi

echo -e "${GREEN}download latest 'package.zip'${NC}"
wget --directory-prefix=./update https://github.com/environmentalinformatics-marburg/rsdb/releases/latest/download/package.zip

if [ ! -f ./update/package.zip ]
then
	echo -e "${RED}could not find 'update/package.zip', abort. (no changes performed)${NC}"
	exit 2
fi

echo -e "extract 'package.zip'"
unzip ./update/package.zip -d ./update
echo -e "${GREEN}download update done.${NC}"

for i in "${update_folders[@]}"; do
	#echo "$i"
	if [ ! -d ./update/$i ]
	then
		echo -e "${RED}folder '$i' is missing in update, abort. (no changes performed)${NC}"
		exit 3
	fi
done

for i in "${update_files[@]}"; do
	#echo "$i"
	if [ ! -f ./update/$i ]
	then
		echo -e "${RED}file '$i' is missing in update, abort. (no changes performed)${NC}"
		exit 4
	fi
done

if [ ! -d ./backup ]
then
	mkdir ./backup
fi

if [ ! -d ./backup ]
then
	echo -e "${RED}could not create 'backup' folder, abort. (no changes performed)${NC}"
	exit 5
fi

backup=./backup/$timestamp

if [ -d $backup ]
then
	echo -e "${RED}backup folder '$backup' already exists, abort. (no changes performed)${NC}"
	exit 6
fi

mkdir $backup
if [ ! -d $backup ]
then
	echo -e "${RED}could not create backup folder '$backup', abort. (no changes performed)${NC}"
	exit 7
fi

for i in "${update_folders[@]}"; do
	#echo "$i"
	if [ -d ./$i ]
	then
		mv ./$i $backup
	else
		echo -e "missing folder for backup '$i'. continue."	
	fi
done

for i in "${update_files[@]}"; do
	#echo "$i"
	if [ -f ./$i ]
	then
		mv ./$i $backup
	else
		echo -e "missing file for backup '$i'. continue."	
	fi
done

for i in "${update_folders[@]}"; do
	#echo "$i"
	if [ -d ./$i ]
	then
		echo -e "${RED}folder '$i' still exists after backup, abort. (some folders/files may be moved to backup already)${NC}"
		exit 8
	fi
done

for i in "${update_files[@]}"; do
	#echo "$i"
	if [ -f ./$i ]
	then
		echo -e "${RED}file '$i' still exists after backup, abort. (some folders/files may be moved to backup already)${NC}"
		exit 9
	fi
done
echo -e "${GREEN}backup done. ('$backup')${NC}"

for i in "${update_folders[@]}"; do
	#echo "$i"
	mv ./update/$i ./
done

for i in "${update_files[@]}"; do
	#echo "$i"
	mv ./update/$i ./
done

for i in "${update_folders[@]}"; do
	#echo "$i"
	if [ -d ./$i ]
	then
		echo -e "updated folder: ${WHITE}$i${NC}"
	else
		echo -e "${RED}folder '$i' has not beend updated (missing), abort. (some updates may have been performed, your should revert to a backup))${NC}"
		exit 10
	fi
done

for i in "${update_files[@]}"; do
	#echo "$i"
	if [ -f ./$i ]
	then
		echo -e "updated file:   ${WHITE}$i${NC}"	
	else
		echo -e "${RED}file '$i' has not beend updated (missing), abort. (some updates may have been performed, your should revert to a backup))${NC}"
		exit 11
	fi
done
echo -e ""
echo -e "${GREEN}update done. backup in '$backup'${NC}"
echo -e ""
exit 0

