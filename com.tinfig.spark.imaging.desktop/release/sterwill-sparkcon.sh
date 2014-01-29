#!/bin/sh

setparams()
{
  for device in /dev/video* ; do
    uvcdynctrl -s "$1" "$2" -d  $device
  done
}

# No auto focus
setparams "Focus, Auto" 0

# Focus infinite
setparams "Focus (absolute)" 0

# Disable auto exposure (1=disable, 3=enable)
setparams "Exposure, Auto" 1

# 156 is the brightest that still gives 30 fps
setparams "Exposure (absolute)" 156

java -Xmx1000M -cp javacv-linux-x86_64.jar:sterwill-sparkcon.jar com.tinfig.spark.imaging.desktop.Main $@
