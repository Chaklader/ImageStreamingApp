

    ###############################################
    # gRPC micro-servise load balance using Nginx #
    ###############################################

    # load the code in the IntellIJ, go in the Maven/ Plugins section to the right-side 
    Click protobuf -> protobuf:compile
    
    # In the maven Life cycle section, 
    click install and package  
   
    # Alternatively, we can use the following commands in th project root as well
    # mvn clean install && mvn clean install 
    
   
    # The above command will create a JAR file to the targte folder named image-streaming-app-0.0.1-SNAPSHOT.jar
    # Copy the JAR file to the dekstop or any convininet location of choice. 
    
    # test the JAR locally and make sure its working file
    java -Xmx700m -jar image-streaming-app-0.0.1-SNAPSHOT.jar

    # this URL should also work locally provides the byte[] to the borswer
    http://localhost:8080/stream?imageUrl=https://static01.nyt.com/images/2020/10/04/world/04london-dispatch-top/merlin_177483009_99edefd2-bf5f-4eb6-98b8-02d143e8181d-jumbo.jpg?quality=90&auto=webp


    # Login to the AWS console and create Amaozn EC2 1 linux instance (I write the bash command particularly for this linux). For the security group, we will use,
    
        Type            Port      Source 
        SSH             22        My IP
        Custom TCP      8080      Anywhere
        HTTP            80        Anywhere
        HTTPS           443       Anywhere
    
    # I have used other as default settings. Eventually, We will use 3 instances where 2 will run the app and 1 will load balance them using 
    # the round robin algorithm.  
    
    # Edit the ~/.ssh/config file:
    
    # we can also use vim if you want. 
    sudo nano ~/.ssh/config
    
    # Copy/ paste the following to the file:
    
    ```
        Host app1
          HostName ec2-52-14-xxx-xx.us-east-2.compute.amazonaws.com
          IdentityFile ~/Documents/arefe.pem
          User ec2-user
        
        
        Host app2
          HostName ec2-3-19-xx-xx.us-east-2.compute.amazonaws.com
          IdentityFile ~/Documents/arefe.pem
          User ec2-user
        
        
        Host lb
          HostName ec2-13-59-xx-xx.us-east-2.compute.amazonaws.com
          IdentityFile ~/Documents/arefe.pem
          User ec2-user
          
    ```
          
          
    # In the former steps, we created the app1 and we need to provide the correct Public IPv4 DNS to the HostName
    # IdentityFile will be the .pem key for the instance. We will edit the app2 and lb later on after we create them.

    
    # login to the linux console
    ssh app1 
    
    # we need to install the Java, port the JAR file here and make the app running in the following steps
            
    ```
    
        #!/bin/bash
       
        # upgrade machine
        sudo yum update -y
        
        # install java 8 jdk
        sudo yum install -y java-1.8.0-openjdk-devel
        
        # set java jdk 8 as default (set #2 option here)
        sudo /usr/sbin/alternatives --config java
        
        # only one option, so press enter here
        sudo /usr/sbin/alternatives --config javac
        
        # verify java 8 is the default
        java -version
        cd /home/ec2-user
    
    ```


    # open another terminal and take the JAR file to the remote linux machine
    # The .pem file and JAR file needs to be in the same location and we need to use the correct Public IPv4 DNS for the instance
    
    scp -i arefe.pem image-streaming-app-0.0.1-SNAPSHOT.jar ec2-user@ec2-xx-14-xxx-28.us-east-2.compute.amazonaws.com:~/.
    
    # make sure we have the JAR file here
    ls 
    
    # make sure JAR file is running fine
    java -Xmx700m -jar image-streaming-app-0.0.1-SNAPSHOT.jar
    
    
    # we need to persist the Java app so when we start the EC2 instance, it will run automatically. Please, copy and run the 
    # commands provided below in the linux terminal:


    ```
        sudo bash -c 'cat << \EOF > /etc/init.d/ec2sampleapp
        
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
        EOF'
    ```

    # we will set the permission, persist the changes 
    sudo chmod +x /etc/init.d/ec2sampleapp
    
    # apply across reboots
    sudo chkconfig --add ec2sampleapp
    sudo chkconfig ec2sampleapp on
    
    # reboot instance 
    sudo reboot
    
    ##################################################################################################
    ##################################################################################################
    
    
    # Note: I havn't provided how to create AIM - there are many posts for that.
    
    # Create an AIM using the EC2 instance and make 2  EC2 instances using the AIM just created and name them app2 
    # and lb. Just for info, we dont need to have the Java installed inside the lb EC2 instance and create that 
    # manually as well. Please, update the ~/.ssh/config file accordingly as we did earlier. Test them with the ssh
    # if we can login there. If we can login, we are good to move on. 

    # edit for the app2 and lb
    sudo nano ~/.ssh/config     

    
    # login to the lb EC2 instance that we will use for the load balancing
    ssh lb
    
    # We need to install the Nginx to the EC2 instance. At first, find the OS info if want to see: 
    cat /etc/os-release 

    
    # we need to add centos version NGINX yum repository
    sudo nano /etc/yum.repos.d/nginx.repo
    
    # paste the centos version to the file
    
    ```
        [nginx]
        name=nginx repo
        baseurl=https://nginx.org/packages/centos/7/$basearch/
        gpgcheck=0
        enabled=1
        
    ```    
       
    sudo yum update -y
    sudo yum install nginx 
    
    # we can use the commands to start, stop and reload nginx

    sudo service nginx start 
    sudo service nginx stop
    sudo service nginx reload 

    # test the ningx in the Public IPv4 DNS for the lb instance to the browser. If we see the Nginx home, we are good.
    
    # we can also edit the default index file for the nginx here
    sudo nano  /usr/share/nginx/html/index.html
    
    # see if the config is coming
    cat  /etc/nginx/nginx.conf
    
    # delete the default file as we write our own
    sudo rm  /etc/nginx/nginx.conf
    
    # use the public IP for the lb EC2 instance and craete a domain (say, xxx.ddns.net) for the testing in the site: https://my.noip.com 
    
    # we need to install certbot for the SSL encryption
    
    sudo curl -O https://dl.eff.org/certbot-auto    
    sudo chmod +x certbot-auto

    # please change the domain name accrdingly that you created earlier 
    sudo ./certbot-auto  certonly --standalone -d xxx.ddns.net
    
    # the above command will print the key location in the terminal like below. Copy and save them as we need them later. 
    
    # Your certificate and chain have been saved at: /etc/letsencrypt/live/xxx.ddns.net/fullchain.pem
   
    # create the config file. I use nano as find vim is not easy 
    sudo nano /etc/nginx/nginx.conf
    
    
    #########
    # NGINX #
    #########
    
    # Please edit all server IPs, domain and key location accordingly. 
    
        ```
            http {
                
                upstream allbackend {
                    #round robin algo used 
                    # Use the private IP for app1 and app2 for faster connection
                    
                    server 172.31.27.125:8080;
                    server 172.31.26.81:8080;
                }
                
                server {
                      listen 80;
                      listen 443 ssl http2;
                      
                      server_name xxx.ddns.net;
            
                      ssl_certificate /etc/letsencrypt/live/xxx.ddns.net/fullchain.pem;
                      ssl_certificate_key /etc/letsencrypt/live/xxx.ddns.net/privkey.pem;
            
                      ssl_protocols TLSv1.3;
            
                      location / {
                          proxy_pass http://allbackend/;
                      }
                 }
            
            }
            
            events { } 
        
        ```
        
    # test the nginx config if all good. This should provide no error. 
    sudo service nginx configtest
    
    # we should get the byte[] from the domain like https://xxx.ddns.net/stream?imageUrl=myCustomImageURL
    
    # Create a CouldFormer stack and generate the YAML file for use. There are a lot of post provide the instruction In case, you can't do it, I will help you. 