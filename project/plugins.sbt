addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.0")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.7.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")

// scalastyle 
resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

// scoverage
resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)
