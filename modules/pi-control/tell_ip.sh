#!bin/bash
hostname -I | xargs | sed 's/ / and /g' | sed 's/\./ point /g' | sed 's/\:/ colon /g'
