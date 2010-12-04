Netty tutorials - handshaking
=============================

This is a tutorial that demonstrates how to factor out handshaking logic into a specialized client/server handshake handler so that it can be removed as soon as it's unnecessary, keeping the final handlers as simple as they should be. 

It's the backing code for the post at http://bruno.biasedbit.com/blag/2010/07/15/handshaking-tutorial-with-netty/