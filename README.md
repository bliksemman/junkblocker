# junkblocker

Do you find yourself spending too much time on social media? Are you
checking Facebook, Twitter, Youtube or other time sinks more than you
really like?

Maybe Junkblocker can be of help. Junkblocker is a DNS proxy that has
filtering capabilities. The inteded purpose is to block junk; ads,
analytics and time wasters like social media and news sites.

Allow access to Facebook and Twitter during breakfast and Lunch but
not during working hours? Sure, Junkblocker can do that!


## Installation

Download from [GitHub](https://github.com/bliksemman/junkblocker/releases).

## Usage

A config file needs to be created to run the server. Copy the
following contents to `myconfig.edn`:

    {:port 1153
     ;; :log "log.txt" ;; Remove this to print to stdout
     :resolver "8.8.8.8" ;; The actual DNS resolver
     :blocked [] ;; List of domains to outright block
     :black-list nil ;; Point this to a hosts file with black-listed domains
     :allow-during []} ;; Time based blocking
                    ; Can be something like below:
                    ; {:from "7" :to "9"
                    ;  :domains [
                    ;            "www.nytimes.com"
                    ;            "www.welt.de"
	

The main options are explained below.

### Port

Set this to the port number on which the server should listen. This
should be 53 after tests have be done. Do not that this requires super
user / root priviliges.

### Resolver

This sets the DNS server that is used to do the actual lookups.

### Blocked

Add domains to this list to black list them. Each domain should be
quoted and space seperated ("example.com" "another.example.com").

### Blacklist

This can be set to file which contains blacklisted
domains. Junkblocker can read files in "/etc/hosts" format. This means
it can directly use files from public lists like
[StevenBlack](https://github.com/StevenBlack/hosts).

Run the next to command to get a nice starting point:

	$ wget https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/fakenews-gambling-porn/hosts -O blacklist.txt


### Allow-during

Time based blocking is done with this option. Any site listed here is
blocked unless there is at least one match with it's allowed
time-window's.

The options is a list of settings in the following format:

	{:from "7" :to "9"
	 :domains [
	           "www.nytimes.com"
	           "www.welt.de"

The `from` and `to` options specify the time range in 24 hour
format. Minutes are allowed using regular notation (`7:45`).

Similar to the `blocked` option the `domains` option lists the domains
that should be managed (blocked / allowed).

It is OK to have the same domain in multiple different
time-ranges. This can be used to allow a news site during morning and
evenings.


## Starting the server

The server can be run using Java with this command:

	$ java -jar junkblocker.jar -c myconfig.edn

## Testing the server

The easiest way to test if everything works OK is to use the `dig`
command. To test nytimes.com for instance (assuming the server runs on
localhost at port 1153):


	dig @127.0.0.1 -p 1153 nytimes.com


If the nytimes.com domain is blacklisted somehow there should be a
response like:

    ; <<>> DiG 9.9.7-P3 <<>> nytimes.com
    ;; global options: +cmd
    ;; Got answer:
    ;; ->>HEADER<<- opcode: QUERY, status: NXDOMAIN, id: 6634
    ;; flags: qr rd ra ad; QUERY: 1, ANSWER: 0, AUTHORITY: 0, ADDITIONAL: 1


When a site is blocked there should be a `status: NXDOMAIN`. In case a
site works normally more output will appear with IP addresses.


## Inteded usage

Install this on a Raspberry PI or other computer. Set your router to
use this DNS server instead of the default ISP's. After that any
device on the (Wifi) network will get blocking regardless of the app.


## License

Copyright © 2018 Jeroen Vloothuis

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
