# Gatling v2.2.3 LoadTest using custom reporter & threads                        
                                                                                 
I do the same kind of crazy stuff with JMeter, but since JMeter is actually a thread-based tool it's not so much abuse as it is just "advanced", I guess ^\_^;
                                                                                 
                                                                                 
I've seen lots of posts online about making custom Actions for Gatling so they can get the data to show up in the simulation.log and thus in the generated visual graphs. I'm perfectly happy using JMeter, but the gatling challenge was too much to resist since so many people seem to be asking for it. So here we go! Just want people to know what's possible, I have no idea if this is a good idea or not.
                                                                                 

![Screenshots](/screenshots/gatlingreport.png?raw=true)

                                                                                 
## GenericReporter.scala                                                          
                                                                                 
This is the file most people will probably be interested in. I tried to make a custom Action, but after digging through gatling's code I realized the only thing most people really need is access to an instance of ```io.gatling.core.stats.DataWritersStatsEngine``` to be able to call its ```logResponse``` method, which is how data ends up in the simulation.log file. If you just include this one file in your project, and look at how I use it, then you'll be all set. Note that you must include the newest jython standalone jar. You don't have to write any jython code yourself, I just use the jython engine to allow reaching private gatling methods that ultimately gave me what I needed. You might be thinking Java's standard reflection, field.setAccessible=true, would work... but it doesn't for some reason. If you find a way, let me know.

## The Jython & JRuby threads

WARNING: While the method I used to create the GenericReporter is an unsupported hack, the fact I'm using threads is a violation of the spirit of Gatling. Gatling, very unlike JMeter, does not create a thread for each Vuser. It uses something called Java Message Service (JMS), which they claim is much lighter weight than threads thus allowing for a load testing tool that requires less resources. So now I've gone around them and made threads anyway, which isn't how Gatling was intended to be used. Concerns that are normally just for threaded loadtesting tools like JMeter are now of concern in Gatling. Example, to use this at scale you now must be sure you're allowed enough open file descriptors. But if you know that your loadtest generating system is powerful enough to run JMeter then it'll be fine for this too. Just be sure to raise open-file-descriptors to max, like 65535 or something. Also the java process, in my opinion, should be raised to 80% of totally memory on the machine. For linux people, editing gatling.sh you'll see something like ```-Xmx1G```. 1G for 1 gig of RAM. Raise it to something closer to 80% of the total available on machine. 

>SIDENOTE: The fact that JMS is used means that all Vusers are in one single thread. This also means that if any one user calls sleep, it hangs the _whole_ thread. So a scenario with 10 vusers each sleeping for 1 second will not result in all 10 users finishing at the sime time for a total scenario-runtime of 1 second. Each sleep will block all the other sleeps from starting so it's going to be each user sleeping sequentially, not in parellel. So the whole scenario's total runtime will be 10 seconds. This goes for any kind of blocking btw, waiting on a Queue or a semaphore will block _all_ the vusers. The way to get around that is by creating threads, but as I've said that probably goes against the idea behind Gatling's usage of JMS.  

## How to run

After running the testcode in this repo, you'll have an idea how to go ahead and do it in your own tests... or decide that this approach is not correct and go use JMeter or something. Or maybe the Gatling devs will make some official way to do this stuff. I dunno.


--- First, I assume you've downloaded gatling-charts-highcharts-bundle-2.2.3. Go to that directory and put all the files in this repo into the relative paths on your machine. The ```gatling_narau.py```(p/jython) and ```gatling_narau.rb```(JRuby) file should be in your gatling root folder and the 4 .scala files should be in your ```user-files/simulations```. Next, in your gatling's ```lib``` folder you need the jython & jruby jars. I have ```jython-standalone-2.7.0.jar``` and ```jruby-complete-9.0.5.0.jar``` but whatever the newest is should be fine. Now that you've done this, go to your terminal and do this:

```
pikachu@POKEMONGYM ~/Forwardz/gatling-charts-highcharts-bundle-2.2.3 $ ./bin/gatling.sh -nr -m -s Narau

```

Normally, the option ```-nr``` stops reports from being logged but ```GenericReporter.scala``` doesn't pay attention to settings. If you don't want to log stuff, then modify your test code to avoid calling the reporter. Maybe check an Environment variable before calling logResponse.

You'll see a bunch of print out that looks similar to unittests. e.g., "...F...FFF..." and some numbers mixed in there from the Ruby script. After about 180 seconds, the scenarios will complete and you should see something like this: 

```
All Scenarios done!
Shutting down Reporter and ActorSystem...
Custom Report data should be in folder: Narau-customReporter-1483050399568
```

The graphs won't be generated automatically since this reporter is a hack running outside of Gatling's normal flow, you'll
have to run one more command that reads in those log files to create the graphs. Also, you're actually going to have _TWO_ simulation.log files. Another folder, called something like ```narau-1483050400568```, with a slightly different timestamp and without the ```customReporter``` part of the folderName. What you need to do now is append this file to the customReport one like this: ```cat results/narau-1483050400568/simulation.log >> results/Narau-customReporter-1483050399568/simulation.log``` NOTE: The ```>>```, _DOUBLE_ greater than is very important. If you only put one greaterThan symbol you'll overwrite the 2nd file and lose the data. You want to APPEND one file into the other. Once you've done this, you'll have a complete simulation.log file for the following command:

```
./bin/gatling.sh -nr -m -s Narau -ro  Narau-customReporter-1483050399568
```

The ```-ro``` command tells Gatling to avoid rerunning the test and instead just go read the ```simulation.log``` file located in the folder given in the command line, in the folder ```results```. And that's it, now in that ```Narau-customReporter-1483050399568``` folder should be an ```index.html```

Have fun! \\^o^/
