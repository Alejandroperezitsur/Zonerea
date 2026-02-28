sh
keytool -genkey -v -keystore zonerea.jks -keyalg RSA -keysize 2048 -validity 10000 -alias zonerea -dname "CN=APV Labs, OU=Development, O=APV Labs, L=City, S=State, C=US"