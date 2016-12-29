# Gatling v2.2.3 LoadTest using custom reporter & threads                        
                                                                                 
I do the same kind of crazy stuff with JMeter, but since JMeter is actually a thread-based tool it's not so much abuse as it is just "advanced", I guess ^\_^;
                                                                                 
                                                                                 
I've seen lots of posts online about making custom Actions for Gatling so they can get the data to show up in the simulation.log and thus in the generated visual graphs. I'm perfectly happy using JMeter, but the gatling challenge was too much to resist since so many people seem to be asking for it. So here we go! Just want people to know what's possible, I have no idea if this is a good idea or not.
                                                                                 
                                                                                 
> GenericReporter.scala                                                          
                                                                                 
This is the file most people will probably be interested in. I tried to make a custom Action, but after digging through gatling's code I realized the only thing most people really need is access to an instance of io.gatling.core.stats.DataWritersStatsEngine to be able to call its logResponse method, which is how data ends up in the simulation.log file. If you just include this one file in your project, and look at how I use it, then you'll be all set. Note that you must include the newest jython standalone jar. You don't have to write any jython code yourself, I just use the jython engine to allow reaching private gatling methods that ultimately gave me what I needed. You might be thinking Java's standard reflection, field.setAccessible=true, would work... but it doesn't for some reason. If you find a way, let me know.

>The Jython & JRuby threads

WARNING: While the method I used to create the GenericReporter is an unsupported hack, the fact I'm using threads is a violation of the spirit of Gatling. Gatling, very unlike JMeter, does not create a thread for each Vuser. It uses something called Java Message Service (JMS), which they claim is much lighter weight than threads thus allowing for a load testing tool that requires less resources. So now that I've gone around them and made threads anyway, which I'm sure they very much dislike and won't support in the foreseeable future.

>HOW TO RUN THE DEMO CODE IN THIS REPO

After running the testcode in this repo, you'll have an idea how to go ahead and do it in your own tests... or decide that this approach is not correct and go use JMeter or something. Or maybe the Gatling devs will make some official way to do this stuff. I dunno.


--- First, I assume you've downloaded gatling-charts-highcharts-bundle-2.2.3. Go to that directory and put all the files in this repo into the relative paths on your machine. The ```gatling_narau.py```(p/jython) and ```gatling_narau.rb```(JRuby) file should be in your gatling root folder and the 3 .scala files should be in your ```user-files/simulations```. Next, in your gatling's ```lib``` folder you need the jython & jruby jars. I have ```jython-standalone-2.7.0.jar``` and ```jruby-complete-9.0.5.0.jar``` but whatever the newest is should be fine. Now that you've done this, go to your terminal and do this:

```
pikachu@POKEMONGYM ~/Forwardz/gatling-charts-highcharts-bundle-2.2.3 $ ./bin/gatling.sh -nr -m -s Narau

```

Normally, the option ```-nr``` stops reports from being logged but ```GenericReporter.scala``` doesn't pay attention to settings. If you don't want to log stuff, then modify your test code to avoid calling the reporter. Maybe check an Environment variable before calling logResponse.

You'll see a bunch of output from the threads, the test will run for a minute or so... then you should see a line similar to:

```
All Scenarios done!
Shutting down Reporter and ActorSystem...
Custom Report data should be in folder: Narau-customReporter-1483050399568
```

The graphs won't be generated automatically since this reporter is a hack running outside of Gatling's normal flow, you'll
have to run one more command that reads in those log files to create the graphs.

```
./bin/gatling.sh -nr -m -s Narau -ro  Narau-customReporter-1483050399568
```

The ```-ro``` command tells Gatling to avoid rerunning the test and instead just go read the ```simulation.log``` file located in the folder given in the command line, in the folder ```results```. And that's it, now in that ```Narau-customReporter-1483050399568``` folder should be an ```index.html```


Have fun! \^o^/







 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
