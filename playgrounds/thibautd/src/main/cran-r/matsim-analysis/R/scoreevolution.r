plot.score.evolution <-
	function( the.file="ScoreStats.dat",
			 dataset=read.table( the.file , header=T , sep="\t" ),
			 group="all",
			 col.max="green",
			 col.exec="black",
			 col.min="red",
			 legend.pos="bottomright",
			 xlab="Iteration",
			 ylab="Score",
			 bty="n",
			 ...) {
	attach( dataset )
	the.lines <- groupId == group
	plot( iter[ the.lines ] , max[ the.lines ] , type="l" , col=col.max ,
		 ylim=c(min( min[ the.lines ] ) , max( max[ the.lines ] )),
		 xlab=xlab, ylab=ylab,
		 bty=bty,
		 ... )
	lines( iter[ the.lines ] , min[ the.lines ] , col=col.min , ... )
	lines( iter[ the.lines ] , exec[ the.lines ] , col=col.exec , ... )
	legend( legend.pos ,
		   c( "avg. min" , "avg. executed" , "avg. max") ,
		   col=c(col.min, col.exec, col.max ),
		   box.lty=0,
		   lty=1 )
	detach()
}
