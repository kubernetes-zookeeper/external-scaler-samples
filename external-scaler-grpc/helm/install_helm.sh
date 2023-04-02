#!/bin/bash

export YUM=yum
export SUDO=sudo


export OS_DISTRIBUTOR_ID=`awk ' BEGIN { FS="=" }  /^ID=/ { print $2 } ' /etc/os-release | tr [A-Z] [a-z] | tr -d '"'`
echo "OS_DISTRIBUTOR_ID: [$OS_DISTRIBUTOR_ID]"

if [[ $OS_DISTRIBUTOR_ID != "centos" && $OS_DISTRIBUTOR_ID != "ubuntu" && $OS_DISTRIBUTOR_ID != "amzn" ]] 
then
	echo "Unsupported OS distribution: [$OS_DISTRIBUTOR_ID]."
	exit -1
fi

if [[ $OS_DISTRIBUTOR_ID == "ubuntu" ]]
then
export YUM=apt-get
else
export YUM=yum
fi



if [[ $OS_DISTRIBUTOR_ID == "amzn" ]]
then

    echo "curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 > /tmp/get_helm.sh"
    curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 > /tmp/get_helm.sh

    echo "chmod 700 /tmp/get_helm.sh"
    chmod 700 /tmp/get_helm.sh

    echo "/tmp/get_helm.sh"
    /tmp/get_helm.sh

else

    echo "${SUDO} ${YUM} -y install epel-release"
    ${SUDO} ${YUM} -y install epel-release
    
    echo "${SUDO} ${YUM} -y install snapd"
    ${SUDO} ${YUM} -y install snapd
    
    echo "${SUDO} systemctl enable --now snapd.socket"
    ${SUDO} systemctl enable --now snapd.socket
    
    echo "${SUDO} ln -s -f /var/lib/snapd/snap /snap"
    ${SUDO} ln -s -f /var/lib/snapd/snap /snap
    
    echo "export PATH=$PATH:/var/lib/snapd/snap/bin"
    export PATH=$PATH:/var/lib/snapd/snap/bin
    
    echo "${SUDO} snap install helm --classic"
    ${SUDO} snap install helm --classic
    
    echo "${SUDO} snap refresh  helm"
    ${SUDO} snap refresh  helm
    
fi
