## TeamCity Remote Run


The tool is designed to simplify the hurdle of TeamCity remote runs.
Use the `rr` tool to solve your issues

This tool contains two elements
 - `teamcity-rr` (deprecated) initial basic tool for starting personal builds on TeamCity
 - `r-view` change-set management tool for our development process.  


### r-view

This is a changelist management tool, designed for a specific use-case,
let us know if it makes sense for your scenarios, we'll make it more 
adjustable. 

With the `r-view` it's easy to track a set of _actively developed patches_ in branches. 
each branch is rebased automatically with master on Git update. The tool tracks
unique commits from each branch (even after the branch is merged). 

Every _actively developed patch_ is shown with a link to view the changes on Space (we can customize) 

There is an action to trigger a safe-push on a _actively developed patch_ (it rebases the branch automatically before push) 


The main aim of this utility is to minimize the time spend in Git management, when
one is working on several patches in parallel. It looks here that a small diviation
from the Git standard workflow helps to win some minutes

#### Commands

Please install and call `r-view` command. This changes too fast right now to be included here.

#### Building 

Use `./gradlew :r-view:installToOs` to install the tool in the system path (tested only on macOS)


### teamcity-rr (deprecated)

#### Commands

`teamcity-rr run` -- starts new remote run from the current folder and shows information on the console

`teamcity-rr show` -- lists all running remote runs


#### Building

The project depends on the `teamcity-rest-client` fork. 
Please checkout the
`https://github.com/jonnyzzz/teamcity-rest-client`
repository and select the `teamcity-rr` branch!


### License

Both tools are available on under the Apache 2.0 license, 
please refer to the [LICENSE](LICENSE) file for details.

### Contribution

Your contribution is welcome, please create a pull request and/or
talk to us
