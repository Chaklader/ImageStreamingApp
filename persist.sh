#!/bin/bash

cat << \EOF > /etc/init.d/ec2sampleapp

# EC2 Sample App    Init script for EC2 Sample App
#
# chkconfig: 345 99 76
# processname: ec2sampleapp

# the location of the JAR file is provided
APP_EXEC="/usr/bin/java -Xmx700m -jar /home/ec2-user/image-streaming-app-0.0.1-SNAPSHOT.jar"
NAME=ec2sampleapp
PIDFILE=/var/run/$NAME.pid
LOG_FILE="/home/ec2-user/$NAME.log"
SCRIPTNAME=/etc/init.d/$NAME
RETVAL=0

start() {
    echo "Starting $NAME..."
    $APP_EXEC 1>"$LOG_FILE" 2>&1 &
    echo $! > "$PIDFILE"
    echo "$NAME started with pid $!"
}

status() {
    printf "%-50s" "Checking $NAME..."
    if [ -f $PIDFILE ]; then
        PID=$(cat $PIDFILE)
        if [ -z "$(ps axf | grep ${PID} | grep -v grep)" ]; then
            printf "Process dead but pidfile exists"
        else
            echo "Running"
        fi
    else
        printf "Service not running"
    fi
}

stop() {
    printf "%-50s" "Stopping $NAME"
        PID=`cat $PIDFILE`
    if [ -f $PIDFILE ]; then
        kill -HUP $PID
        printf "%s\n" "Ok"
        rm -f $PIDFILE
    else
        printf "%s\n" "pidfile not found"
    fi
}

case "$1" in
    start)
    start
    ;;
    status)
    status
    ;;
    stop)
    stop
    ;;
    restart)
        $0 stop
        $0 start
    ;;
    *)
        echo "Usage: $0 {status|start|stop|restart}"
        exit 1
    ;;
esac
EOF