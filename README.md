[![Build Status](https://travis-ci.org/seyfahni/OnlineTime.svg?branch=master)](https://travis-ci.org/seyfahni/OnlineTime)

# OnlineTime

This plugin measures the total time players are connected to your server network.

This plugin is based on [Mr_Minecraft15's plugin OnlineTime](https://github.com/MarvinKlar/OnlineTime), but I've more or less completely rewritten everything up to the point where it is barely noticeable.

## Features:
- Every message (including prefix) is customizable!
- Store data in YAML or MySQL
- extreme multiprogramming, zero blocking operations on I/O-threads cause zero lag
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) support

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

## PlaceholderAPI
| Placeholder                 | Description                                       | Example (with 5d 2h 7min)   |
| --------------------------- | ------------------------------------------------- | --------------------------- |
| `%onlinetime_string%`       | formatted string (same as when using /onlinetime) | `5 days 2 hours 7 minutes`  |
| `%onlinetime_only_seconds%` | seconds, as in formatted string (without unit)    | `0`                         |
| `%onlinetime_only_minutes%` | minutes, as in formatted string (without unit)    | `7`                         |
| `%onlinetime_only_hours%`   | hours, as in formatted string (without unit)      | `2`                         |
| `%onlinetime_only_days%`    | days, as in formatted string (without unit)       | `5`                         |
| `%onlinetime_only_weeks%`   | weeks, as in formatted string (without unit)      | `0`                         |
| `%onlinetime_only_months%`  | months, as in formatted string (without unit)     | `0`                         |
| `%onlinetime_only_years%`   | years, as in formatted string (without unit)      | `0`                         |
| `%onlinetime_all_seconds%`  | whole time in seconds                             | `439620`                    |
| `%onlinetime_all_minutes%`  | whole time in minutes                             | `7327`                      |
| `%onlinetime_all_hours%`    | whole time in hours                               | `122`                       |
| `%onlinetime_all_days%`     | whole time in days                                | `5`                         |
| `%onlinetime_all_weeks%`    | whole time in weeks                               | `0`                         |
| `%onlinetime_all_months%`   | whole time in months                              | `0`                         |
| `%onlinetime_all_years%`    | whole time in years                               | `0`                         |

## Planned / Ideas:

See [enhancement labeled issues](https://github.com/seyfahni/OnlineTime/labels/enhancement).

## License:
This plugin is licensed under the MIT license. For further information see [LICENSE](https://github.com/seyfahni/OnlineTime/blob/master/LICENSE).
