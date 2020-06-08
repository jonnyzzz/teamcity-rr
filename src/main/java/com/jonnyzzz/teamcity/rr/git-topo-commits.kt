package com.jonnyzzz.teamcity.rr

fun main() {

    val commits =
            """b2e3deda99559dff9d0420980f38b14dd6c62b90  39b9efa4f5e4af596669de85f6009a242ca4fb6d  2d01ceae403033c4a6fe47960a52f93caf2c303a  fd9ff76455b1fcfd29e1cef867f891f169e2c60b  00ef078ac8f8d8b7d1c8344125367a3cca4acfbc  dd4df24867e30d4b03bd5599916ad70a923a153a  d510ca732560ce68988330e1b0ffe000a0829ef0  ac404a7691e3f596a0a6615b22a596b02dd54f73  d27c7ae708d15e1dc59d7f45a08bd3a261fbe567  e8b02a9c7dfafc819c18282ad2a184966315aa1c  8036ec0643ea709bb8c3766ff6cebe5f15dc9ffc  81143c217a8f4c3370cdd8e266c3f70a51558db3  af706db8bc417f83b48686b96866c77c949eb77d  fa5c9dccf7d6e73d34ad5e2d45b423af8b787811  66b0e16b71af53da4b9cc3130f72bb51cc316987  7904a623791cf46f0f51615a0da395cae579e92b  d041bfa82d4aeb764772923cfae1880c8f2c0ec2  20500109ab28d9ca741c10ac2f7517e7b3f92cb8  ff7cc35b21757533d2152919513f5c45c2b078ae  b219582cbee18e4fe32048639be26e77661b359f  cd94ddbe3472a4dd5b68805be76c158648b1e7b1  2ae0caedd8c1a707bfa269bc32d179dcae884272  0fe6e1e6a2f9a629676c8e11fbd6ab28a731cb42  d382b602e467d4518d0134653518fad7dd03b1a7  2637aeeecd7086ea7d53ecd9bf554a60429d69cf  98ac591442142db2d1bde04c0f735021a8ce0070  3495230ff1cd38e95526ccbd1415c7eec05d0518  2257bf62e721d101253edd2a072a873ce431b386  a87195625a0a4ed7f338cfa4736d7a5c3b33f601  bc47b3bff98e81c236e47746ffcf480e9c24447d  2c82fbf14642eb41dc8831f9a6d4d2839ad2d805  87965eca551e57c6b0b9a20d20e3df84e45c756f  6caafe9e046b69ad51b3bbd2913f7fb1aace65a0  5552a6fe0a996e11f77424a067de72d02657f9de  f9cf84269830bf7115464e382975fe43c75de280  110d36633bd35b5726e86fa555d419050d05a6a3  f80eb3efdc670d7b7ca6bf4cc2575918ffed73ff  93f1debbee03b487212536aa9163613676bfa96e  d5e4391ede484eb4ccad3c7dce97663aa6a0c742  ef54563a749faf42299b7c08145951bdc31d58b2  33ac5b684dde84e6e2a90870c07ec910a4657ed7  349bec4a55b6c7eacd5515d71e6bbbee59bd922f  5587c451438a5dd08737604d594853e656efab49  d8d1c0f16faa675b8a4da96f8e291a760b94748c  24d77e5e8dfff635ae65403b82ae418f7ea723eb  aa77a4dd818b793dcb0e26f36517c01c7bb4a30b  07854c880a1c7d0c85ae3837dcc86706b8a95271  815b2c6d669dffa7c0ca17375065691696139943  e9d819753c765d7c2f3bb84bc4d4b2d34d89599a  50eb7f4b18085006edcbca3a54a29703d5ff95a4  cc5abd4e44cf95757994345c9f2faea7a9209891  6455f907673cb717673772b5a40a4c8911be3c7f  0704a2cefac57d236385d6a5343aa960ba23d254  fe7c13334a1907a1565a9bb4aca62173383a8e51  63ccde8cfad68432ba05db341a3ecc3ff5a87c63  dd54ab43415c39ad638c87ded45d9765752658ac  86874a5f809b0030c30235174557e9f21f8892fa  e38fbf39ef1d6fc426c765fa5baf6f5978fb927a  2877945786fe6cdb30e3b8db1b5df74689d9f155  7c43e9478e276cb9fc58ef65fe0937977eb5fcf7  4c1e148bcfd3179f5a593a2e9742a4c95a993258  e68a58d1e3a2d24ff56eef0e5b3eb018160ea5e5  a67cf86d8f1638f9a87358d507d059ece38b2036  bad5913204118deda60504a79f6442dff1c117ac  9869a55229537119f63cea9a4bcb6fd5a3bc84fe  d92994917dde6c5f4b3749d05a749b46e4a5d7a8  0e89ae361aa7e48872b250e1aeb2cdee2b05564a  08fcccc72db3420bd1ac41f5ff7f55e7c82f2070  7305359d683e009d796f5dec94d193c54041fbce  528a94e56a1a22876fd9fd38dba049d28e744625  ccc1518bdb8013a0d9be92ea4fb6ebd3d0295244  514114cc275230ea3ea59a4ee623aa479ab236f7"""
                    .split(" ").map { it.trim() }.filter { it.isNotBlank() }
                    .toMutableSet()


    val originalOrder = listGitCommits("master", 8192)

    val sorted = mutableListOf<String>()
    for (commit in originalOrder.reversed()) {
        if (commits.remove(commit)) {
            sorted.add(commit)
        }
    }

    require(commits.isEmpty()) { "alien commits from non-master branch: ${commits.joinToString(", ")}"}

    val withInfo = sorted.map {
        showCommitShort(it)
    }

    println("TopoSorted commits:")
    println(sorted.joinToString(" "))

    println("")
    println("Log:")
    withInfo.forEach { println(it) }

    println("")
    println("diff stat:")
    println(generateDiffStat(sorted))
}