# Performs spectral clustering into k classes for a numpy matrix
# where rows of the matrix represent data examples
# Evan Ettinger 4/20/09

try:
    import numpy as np
except ImportError:
    print 'Import error: numpy module is required to run this script!'
    sys.exit(2)
import math
import random

################################################################
# X                  - numpy N by M data matrix
# cluster_membership - which clusters each datapoint belongs to
# centers            - k x M matrix of the centers of each cluster
#
# Returns an index saying which center the point is closest to
def getCenters(X,cluster_membership,centers):
    #Calculate new centers
    k = np.shape(centers)[0]
    num_points = [0]*k
    retCenters = np.mat(np.zeros(np.shape(centers)))
    for i in range(np.shape(X)[0]):
        indx = cluster_membership[i]
        num_points[indx] += 1
        retCenters[indx,:] += X[i,:]

    for i in range(k):
        retCenters[i,:] /= num_points[i]

    #Calculate maximum shift between centers
    shift = 0
    for i in range(k):
        dist = np.linalg.norm(retCenters[i,:] - centers[i,:])
        if(dist > shift):
            shift = dist

    return retCenters,shift

################################################################
# point      - an Mx1 real vector
# centers    - k x M matrix of the centers of each cluster
#
# Returns an index saying which center the point is closest to
def getMembership(point,centers):
    #Calculate distance to 1st center
    dist = np.linalg.norm(point - centers[0,:])
    indx = 0
    for i in range(1,np.shape(centers)[0]):
        temp = np.linalg.norm(point - centers[i,:])
        if(temp < dist):
            dist = temp
            indx = i
    return indx

################################################################
# X      - numpy N by M data matrix, row vectors are data examples
# k      - number of clusters, k < N
#
# Returns k centers via a furtherst first initialization
def initCenters(X,k):
    M = np.shape(X)[1]
    N = np.shape(X)[0]
    random.seed(42)
    I = random.sample(range(N),1)
    centers = np.mat(np.zeros((k,M)))
    #start with a random sample point as the first center
    centers[0,:] = X[I,:]
    num_centers = 1
    #For the rest of the centers find the data point who has
    # largest min distance to the current centers
    for i in range(1,k):
        ff_dist = 0
        temp_indx = 0
        for j in range(N):
            #Calculate distance from jth point to each center
            # and record j if the smallest of these distances is
            # larger than the current largest.
            min_dist = np.linalg.norm(X[j,:] - centers[0,:])
            for k in range(1,i):
                temp = np.linalg.norm(X[j,:] - centers[k,:])
                if(temp < min_dist):
                    min_dist = temp
            if(min_dist > ff_dist):
                ff_dist = min_dist
                temp_indx = j
        centers[i,:] = X[temp_indx,:]
        
    return centers

################################################################
# X      - numpy N by M data matrix, row vectors are data examples
# k      - number of clusters, k < N
#
# Returns a vector of length N where each element is in {1,...,k}
# indicating the cluster group each datum belongs to
def kmeans(X,k):
    #Initialize cluster centers, using furtherst first
    centers = initCenters(X,k)

    shift = 1
    threshold = 1e-10
    N = np.shape(X)[0]
    cluster_membership = [0]*N
    #While the biggest shift in cluster center is > threshold
    #  run the k-means body loop
    while(shift > threshold):
        #Assign each point to a cluster center
        for i in range(N):
            cluster_membership[i] = getMembership(X[i,:],centers)

        #Recalculate centers
        centers,shift = getCenters(X,cluster_membership,centers)
                
    #Which cluster center is each point with? Return this.
    return cluster_membership

################################################################
# matrix      - numpy N by N similarity matrix
# k           - number of clusters, k < N
#
# Returns a vector of length N where each element is in {1,...,k}
# indicating the cluster group each datum belongs to
def DoSpectralClustering(matrix,k):
    #Symmetrize matrix and remove diagonal
    S = matrix + np.transpose(matrix)
    S = S - np.diag(np.diag(S))

    #Calculate Graph Laplacian L and D^(-1/2)
    D = np.diagflat(np.sum(S,axis=1)).astype(float)
    halfD = np.mat(np.zeros(D.shape))
    for i in range(D.shape[0]):
        halfD[i,i] = math.sqrt(D[i,i])
        # HACK: Make sure we don't have an ill-formed matrix
        #  for the inverse operation to follow
        if(halfD[i,i] == 0):    
            halfD[i,i] = 1e-12
    invHalfD = np.linalg.inv(halfD)
    L = invHalfD*S*invHalfD

    #Calculate eigenvalues/vectors and sort in descending order of eigv
    E,V = np.linalg.eigh(L)
    I = np.argsort(E)
    I = I[::-1].copy()
    V = V[:,I]

    #Do K-means
    groups = kmeans(V[:,0:k],k)
    return groups

