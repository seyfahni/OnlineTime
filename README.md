[![Build Status](https://travis-ci.org/seyfahni/OnlineTime.svg?branch=master)](https://travis-ci.org/seyfahni/OnlineTime)

# OnlineTime

This plugin measures the total time players are connected to your server network.

This plugin is based on [Mr_Minecraft15's plugin OnlineTime](https://github.com/MarvinKlar/OnlineTime), but I've more or less completely rewritten everything up to the point where it is barely noticeable.

## Features:
- Every message (including prefix) is customizable!
- Store data in YAML or MySQL
- extreme multiprogramming, zero blocking operations on I/O-threads cause zero lag

## Commands:
| Command                                            | Description                                      |
| -------------------------------------------------- | ------------------------------------------------ |
| `/onlinetime [playername or uuid]`                 | show online time of player                       |
| `/ot [playername or uuid]`                         | alias for /onlinetime                            |
| `/onlinetimeadmin set [playername or uuid] [time]` | set a players online time to the given amount    |
| `/onlinetimeadmin mod [playername or uuid] [time]` | modify a players online time by the given amount |
| `/onlinetimeadmin reset [playername or uuid]`      | reset a players online time to zero              |
| `/ota (set\|mod\|reset) ...`                       | alias for /onlinetimeadmin                       |

The time argument accepts values as follows:
- just any whole number: interpreted as seconds
- multiple combination of amount with unit: the sum of given amount
- Examples:

| Time Example     | Interpretation                        |
| ---------------- | ------------------------------------- |
| `77`             | 77 seconds or 1 minute and 17 seconds |
| `4h 3min`        | 4 hours and 3 minutes                 |
| `28 days 1 hour` | 28 days and 1 hour                    |
| `2w1d`           | 2 weeks and one day                   |
| `1h -5min`       | 1 hour minus 5 minutes or 55 minutes  |
| `-6 d`           | subtract 6 days (only using modify)   |


## Permissions:
| Permission             | Description                            |
| ---------------------- | -------------------------------------- |
| `onlinetime.see`       | see your recorded online time          |
| `onlinetime.see.other` | see other players recorded online time |
| `onlinetime.admin`     | modify all entries                     |

## Planned / Ideas:

| Priority | Status | Description                                                       |
| -------- | ------ | ----------------------------------------------------------------- |
| LOW      | NEW    | import / data transfer (YAML to MySQL and back)                   |
| LOW      | NEW    | personal language per player (as long as configured)              |
| LOW      | NEW    | conditions for time counting                                      |
| LOW      | NEW    | allow selection of time units to be used (eg. only days an hours) |

## License:
This plugin is licensed under the MIT license. For further information see [LICENSE](https://github.com/seyfahni/OnlineTime/blob/master/LICENSE).
