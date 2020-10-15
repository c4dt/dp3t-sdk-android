To copy the relevant microG libraries:

```
for m in play-services-{base,base-api,base-core,basement,nearby,nearby-api,nearby-core,nearby-core-proto,tasks}
do 
    echo $m
    rm -rf ./dp3t-sdk/$m
    cp -r ../android_packages_apps_GmsCore/$m ./dp3t-sdk/
    echo "/build" > ./dp3t-sdk/$m/.gitignore
done
```
